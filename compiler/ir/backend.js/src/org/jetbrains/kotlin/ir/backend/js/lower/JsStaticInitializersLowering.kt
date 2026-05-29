/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlockBody
import org.jetbrains.kotlin.backend.common.lower.irIfThen
import org.jetbrains.kotlin.backend.common.phaser.PhasePrerequisites
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities.PRIVATE
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.JsCommonBackendContext
import org.jetbrains.kotlin.ir.backend.js.JsLoweredDeclarationOrigin
import org.jetbrains.kotlin.ir.backend.js.correspondingField
import org.jetbrains.kotlin.ir.backend.js.getInstanceFun
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.objectGetInstanceFunction
import org.jetbrains.kotlin.ir.backend.js.staticInitFunction
import org.jetbrains.kotlin.ir.backend.js.staticInitializerProcessed
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.isAny
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.name.Name

/**
 * Moves initializers of static members of the class coming from companion blocks into a static initializer function.
 *
 * Before:
 * ```kotlin
 * class Foo {
 *   companion {
 *     val first = initFirst()
 *   }
 *   companion object {
 *     val second = initSecond()
 *   }
 *   companion {
 *     val third = initThird()
 *   }
 * }
 * ```
 *
 * After:
 * ```kotlin
 * class Foo {
 *   companion {
 *     val Foo$static_init_called = false
 *     Foo$static_init() {
 *       if (Foo$static_init_called) return
 *       first = initFirst()
 *       second = initSecond()
 *       third = initThird()
 *     }
 *   }
 *   companion {
 *     val first: FirstType
 *   }
 *   companion object {
 *     val second: SecondType
 *   }
 *   companion {
 *     val third: ThirdType
 *   }
 * }
 * ```
 */
@PhasePrerequisites(
    ObjectDeclarationLowering::class,
    EnumEntryInstancesLowering::class,
    EnumEntryCreateGetInstancesFunsLowering::class,
)
class JsStaticInitializersDeclarationLowering(private val context: JsCommonBackendContext) : FileLoweringPass {
    companion object {
        val STATIC_FIELD_INITIALIZER by IrStatementOriginImpl
        val STATIC_CLASS_INITIALIZER by IrDeclarationOriginImpl.Synthetic

        const val STATIC_INIT_FUNCTION_NAME = "static_init"
        const val STATIC_INIT_CALLED_PROPERTY_NAME = "static_init_called"
    }

    override fun lower(irFile: IrFile) {
        irFile.acceptVoid(object : IrVisitorVoid() {
            override fun visitFile(declaration: IrFile) {
                declaration.acceptChildrenVoid(this)
            }

            override fun visitClass(declaration: IrClass) {
                processDeclarationContainer(declaration)
                declaration.acceptChildrenVoid(this)
            }
        })
    }

    private fun processDeclarationContainer(container: IrClass) {
        if (container.isEffectivelyExternal()) return

        // A class may already have been processed by a lowering instance bound to another module, see the comment below.
        if (container.staticInitializerProcessed) return

        // Before building child static_init, we need to be sure that all super types are processed in order before the child.
        // This is needed for 2 reasons:
        // 1. To create a call to a parent static_init in the child static_init body.
        // 2. To create child static_init even if the child doesn't have any initializers, but super class has.
        container.dependencySuperClasses.forEach { processDeclarationContainer(it) }

        val hasStaticFieldInitializer = container.declarations.any {
            when (it) {
                is IrEnumEntry -> it.correspondingField?.isStatic == true && it.initializerExpression != null
                is IrField -> it.isStatic && it.initializer != null
                is IrProperty -> it.backingField?.isStatic == true && it.backingField?.initializer != null
                else -> false
            }
        }

        container.staticInitializerProcessed = true
        if (!hasStaticFieldInitializer && container.superClass?.staticInitFunction == null) return

        val initializers = buildList {
            for (declaration in container.declarations) {
                // Special handling of companion objects - if the static_init function is introduced, the Companion_getInstance
                // body should be moved to the static_init body to preserve the correct order of initialization.
                // _getInstance then calls static_init instead.
                if (declaration is IrClass && declaration.isCompanion) {
                    declaration.objectGetInstanceFunction?.let { getInstance ->
                        val body = getInstance.body as? IrBlockBody ?: return@let
                        body.statements.let { statements ->
                            // Relying on the fact that _getInstance always ends with IrReturn
                            addAll(statements.dropLast(1))
                            val irReturn = statements.last()
                            statements.clear()
                            statements.add(irReturn)
                        }
                    }
                    continue
                }

                val [field, initializerBody] = when (declaration) {
                    is IrEnumEntry -> declaration.correspondingField to declaration.initializerExpression
                    is IrField -> declaration to declaration.initializer
                    is IrProperty -> declaration.backingField to declaration.backingField?.initializer
                    else -> continue
                }
                if (field == null || initializerBody == null) continue
                if (!field.isStatic) continue

                add(container.createInitializer(declaration, field, initializerBody.expression))
                field.initializer = null
            }
        }

        // It is important to define stable signature via restrictTo to be able to reference static_init of super class
        // defined in a separate module.
        val [staticInitCalledField, staticInitFunction] = context.irFactory.stageController.restrictTo(container) {
            val initCalledField = createStaticInitCalledField(container)
            val initFunction = createStaticInitFunction(
                container = container,
                origin = STATIC_CLASS_INITIALIZER,
                initCalledVar = initCalledField,
                initializers = initializers
            )
            initCalledField to initFunction
        }

        // Adding static_init declaration after adding its usages to make sure we don't insert usages inside static_init itself
        container.staticInitFunction = staticInitFunction
        container.declarations.addAll(0, listOf(staticInitCalledField, staticInitFunction))
    }

    private fun IrClass.createInitializer(declaration: IrDeclaration, field: IrField, initializer: IrExpression): IrSetField =
        context.irBuiltIns.createIrBuilder(symbol, declaration.startOffset, declaration.endOffset).run {
            irSetField(
                receiver = null,
                field = field,
                value = initializer,
                origin = STATIC_FIELD_INITIALIZER
            )
        }

    private fun createStaticInitCalledField(irClass: IrClass): IrField = context.irFactory.buildField {
        name = Name.identifier("${irClass.name.identifier}$$STATIC_INIT_CALLED_PROPERTY_NAME")
        origin = STATIC_CLASS_INITIALIZER
        type = context.irBuiltIns.booleanType
        visibility = PRIVATE
        isStatic = true
    }.apply {
        parent = irClass
        initializer = context.irFactory.createExpressionBody(
            SYNTHETIC_OFFSET,
            SYNTHETIC_OFFSET,
            JsIrBuilder.buildBoolean(context.irBuiltIns.booleanType, false)
        )
    }

    private fun createStaticInitFunction(
        container: IrClass,
        origin: IrDeclarationOrigin,
        initCalledVar: IrField,
        initializers: List<IrStatement>
    ): IrSimpleFunction {
        val initFunction = context.irFactory.buildFun {
            startOffset = UNDEFINED_OFFSET
            endOffset = UNDEFINED_OFFSET
            this.origin = origin
            name = Name.identifier(container.name.identifier + '$' + STATIC_INIT_FUNCTION_NAME)
            visibility = PRIVATE
            returnType = context.irBuiltIns.unitType
        }
        return initFunction.apply {
            val builder = context.createIrBuilder(symbol, SYNTHETIC_OFFSET)
            parent = container
            body = context.irFactory.createBlockBody(startOffset, endOffset) {
                statements += builder.irBlockBody(this) {
                    +irIfThen(irGetField(null, initCalledVar), irReturnUnit())
                    +irSetField(null, initCalledVar, irBoolean(true))
                }.statements

                container.dependencySuperClasses
                    .mapNotNull { it.staticInitFunction }
                    .forEach { statements += builder.irCall(it.symbol) }

                for (initializer in initializers) {
                    initializer.setDeclarationsParent(initFunction)
                }
                statements += initializers
            }
        }
    }

    private val IrClass.dependencySuperClasses: List<IrClass>
        get() = superTypes
            .filter { !it.isAny() }
            .mapNotNull { it.classOrNull?.owner }
            // In the case of super interfaces, only ones having at least 1 non-abstract member trigger
            // its initialization from the implementing class. See section §3.3 of the KEEP.
            .filter { clazz -> !clazz.isInterface || clazz.declarations.any { it.isNonAbstractInstanceMember() } }

    private fun IrDeclaration.isNonAbstractInstanceMember(): Boolean = when (this) {
        is IrSimpleFunction if isReal && modality != Modality.ABSTRACT && dispatchReceiverParameter != null -> true
        is IrProperty if isReal && modality != Modality.ABSTRACT && (getter ?: setter)?.dispatchReceiverParameter != null -> true
        else -> false // nested classes, companion object, fields, etc. don't count
    }
}

/**
 * Inserts calls to a static initializers function (static_init) into relevant function bodies.
 *
 * @param initializeObjectEnumParent When true, a reference to a nested object inside a class with static initializers will cause
 *  the static_init function of that class to execute. When false, only companion object access would trigger static_init execution.
 *
 * Before:
 * ```kotlin
 * class Foo {
 *   companion {
 *     val Foo$static_init_called = false
 *     Foo$static_init() {
 *       if (Foo$static_init_called) return
 *       first = initFirst()
 *       second = initSecond()
 *       third = initThird()
 *     }
 *   }
 *   companion {
 *     val first: FirstType
 *   }
 *   companion object {
 *     val second: SecondType
 *   }
 *   companion {
 *     val third: ThirdType
 *   }
 * }
 * ```
 *
 * After:
 * ```kotlin
 * class Foo {
 *   constructor() {
 *     Foo$static_init()
 *   }
 *   companion {
 *     val Foo$static_init_called = false
 *     Foo$static_init() {
 *       // ...
 *     }
 *   }
 *   companion {
 *     val first: FirstType
 *   }
 *   companion object {
 *     val second: SecondType
 *     init {
 *       Foo$static_init()
 *     }
 *   }
 *   companion {
 *     val third: ThirdType
 *   }
 * }
 */
@PhasePrerequisites(JsStaticInitializersDeclarationLowering::class)
class JsStaticInitializersUsageLowering(
    private val context: JsCommonBackendContext,
    private val initializeObjectEnumParent: Boolean = true
) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.acceptVoid(object : IrVisitorVoid() {
            override fun visitFile(declaration: IrFile) {
                declaration.acceptChildrenVoid(this)
            }

            override fun visitClass(declaration: IrClass) {
                insertStaticInitCall(declaration)
                declaration.acceptChildrenVoid(this)
            }
        })
    }

    private fun insertStaticInitCall(container: IrClass) {
        if (container.isEffectivelyExternal()) return
        val staticInitFunction = container.staticInitFunction ?: return

        val builder = context.irBuiltIns.createIrBuilder(container.symbol, UNDEFINED_OFFSET, UNDEFINED_OFFSET)
        for (declaration in container.declarations) {
            when (declaration) {
                is IrEnumEntry -> {
                    declaration.getInstanceFun?.let { getInstance ->
                        builder.insertCall(getInstance, staticInitFunction)
                    }
                }
                // Do not insert call to a static_init into static_init itself
                is IrSimpleFunction if declaration == declaration.parentClassOrNull?.staticInitFunction -> continue
                // Do not insert a call to a static_init into an enum constructor, since it would be only accessible from static_init.
                // Redundant re-entrance into static_init pollutes stepping.
                is IrConstructor if (container.isEnumClass || container.isEnumEntry) -> continue
                is IrSimpleFunction, is IrConstructor -> {
                    // If initializeObjectEnumParent is true, call static_init from all objects getInstance
                    // including nested objects. This behavior is K/JS-only and differs from JVM. Kept for compatibility.
                    // Please see KT-83337.
                    if (!initializeObjectEnumParent && declaration.origin == JsLoweredDeclarationOrigin.OBJECT_GET_INSTANCE_FUNCTION) continue
                    if (declaration.dispatchReceiverParameter != null) continue // already initialized when instance was created
                    builder.insertCall(declaration, staticInitFunction)
                }
                // If initializeObjectEnumParent is false, only call static_init from getInstance coming from the companion object.
                // JVM-based behavior, also relevant for Wasm.
                is IrClass if declaration.isCompanion && !initializeObjectEnumParent -> {
                    val getInstance = declaration.objectGetInstanceFunction ?: continue
                    builder.insertCall(getInstance, staticInitFunction)
                }
            }
        }
    }

    private fun DeclarationIrBuilder.insertCall(target: IrFunction, staticInitFunction: IrSimpleFunction) {
        val body = target.body as? IrBlockBody ?: return
        body.statements.add(0, irCall(staticInitFunction.symbol))
    }
}
