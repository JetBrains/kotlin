/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irIfThen
import org.jetbrains.kotlin.backend.common.phaser.PhasePrerequisites
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.JsCommonBackendContext
import org.jetbrains.kotlin.ir.backend.js.correspondingField
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.objectGetInstanceFunction
import org.jetbrains.kotlin.ir.backend.js.staticInitFunction
import org.jetbrains.kotlin.ir.backend.js.staticInitializerProcessed
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irBoolean
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irReturnUnit
import org.jetbrains.kotlin.ir.builders.irSetField
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOriginImpl
import org.jetbrains.kotlin.ir.declarations.IrEnumEntry
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.createBlockBody
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrSetField
import org.jetbrains.kotlin.ir.expressions.IrStatementOriginImpl
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.isAny
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.ir.util.isEffectivelyExternal
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.ir.util.isReal
import org.jetbrains.kotlin.ir.util.setDeclarationsParent
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.name.Name
import kotlin.collections.forEach
import kotlin.collections.plusAssign

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
 *     var static_init_called = false
 *     static_init() {
 *       if (static_init_called) return
 *       static_init_called = true
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
class WebStaticInitializersDeclarationLowering(private val context: JsCommonBackendContext) : FileLoweringPass {
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
        container.staticInitializerProcessed = true

        // Before building child static_init, we need to be sure that all super types are processed in order before the child.
        // This is needed for 2 reasons:
        // 1. To create a call to a parent static_init in the child static_init body.
        // 2. To create child static_init even if the child doesn't have any initializers, but super class has.
        var hasSuperClassWithStaticInitializer = false
        container.dependencySuperClasses.forEach {
            processDeclarationContainer(it)
            if (it.staticInitFunction != null) hasSuperClassWithStaticInitializer = true
        }

        val hasStaticFieldInitializer = container.declarations.any {
            when (it) {
                is IrEnumEntry -> it.correspondingField?.isStatic == true && it.initializerExpression != null
                is IrField -> it.isStatic && it.initializer != null
                is IrProperty -> it.backingField?.isStatic == true && it.backingField?.initializer != null
                else -> false
            }
        }

        if (!hasStaticFieldInitializer && !hasSuperClassWithStaticInitializer) return

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
        name = Name.identifier(STATIC_INIT_CALLED_PROPERTY_NAME)
        origin = STATIC_CLASS_INITIALIZER
        type = context.irBuiltIns.booleanType
        visibility = DescriptorVisibilities.PRIVATE
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
            name = Name.identifier(STATIC_INIT_FUNCTION_NAME)
            visibility = DescriptorVisibilities.PRIVATE
            returnType = context.irBuiltIns.unitType
        }
        return initFunction.apply {
            val builder = context.createIrBuilder(symbol, SYNTHETIC_OFFSET)
            parent = container
            body = context.irFactory.createBlockBody(startOffset, endOffset) {
                with(builder) {
                    statements += irIfThen(irGetField(null, initCalledVar), irReturnUnit())
                    statements += irSetField(null, initCalledVar, irBoolean(true))
                }

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
