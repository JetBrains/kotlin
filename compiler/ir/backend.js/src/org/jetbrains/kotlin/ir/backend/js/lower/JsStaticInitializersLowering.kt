/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlockBody
import org.jetbrains.kotlin.backend.common.lower.irIfThen
import org.jetbrains.kotlin.backend.common.phaser.PhasePrerequisites
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities.PRIVATE
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.JsCommonBackendContext
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.correspondingField
import org.jetbrains.kotlin.ir.backend.js.getInstanceFun
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.objectGetInstanceFunction
import org.jetbrains.kotlin.ir.backend.js.staticInitializer
import org.jetbrains.kotlin.ir.backend.js.staticInitializerProcessed
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
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
 *   constructor() {
 *     Foo$static_init()
 *   }
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
 *     init {
 *       Foo$static_init()
 *     }
 *   }
 *   companion {
 *     val third: ThirdType
 *   }
 * }
 * ```
 */
@PhasePrerequisites(
    ObjectDeclarationLowering::class,
    EnumEntryCreateGetInstancesFunsLowering::class,
)
class JsStaticInitializersLowering(private val context: JsCommonBackendContext) : FileLoweringPass {
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

        // Before building child static_init, we need to be sure that all super classes are processed in order before the child.
        // This is needed for 2 reasons:
        // 1. To create a call to a parent static_init in the child static_init body.
        // 2. To create child static_init even if the child doesn't have any initializers, but super class has.
        container.superClass?.let { processDeclarationContainer(it) }

        val builder = context.irBuiltIns.createIrBuilder(container.symbol, SYNTHETIC_OFFSET, SYNTHETIC_OFFSET)
        val staticFieldsToInitializers = buildMap {
            for (declaration in container.declarations) {
                val [field, initializerBody] = when (declaration) {
                    is IrEnumEntry -> declaration.correspondingField to declaration.initializerExpression
                    is IrField -> declaration to declaration.initializer
                    is IrProperty -> declaration.backingField to declaration.backingField?.initializer
                    else -> null to null
                }
                if (field == null || initializerBody == null) continue
                if (!field.isStatic) continue
                put(declaration, field to initializerBody.expression)
            }
        }

        val initializers = buildList {
            for (declaration in container.declarations) {
                when (declaration) {
                    in staticFieldsToInitializers -> {
                        staticFieldsToInitializers[declaration]?.let { [field, initializer] ->
                            val initBuilder = context.irBuiltIns.createIrBuilder(
                                container.symbol, declaration.startOffset, declaration.endOffset
                            )
                            add(
                                initBuilder.irSetField(
                                    receiver = null,
                                    field = field,
                                    value = initializer,
                                    origin = STATIC_FIELD_INITIALIZER
                                )
                            )
                            field.initializer = null
                        }
                    }
                    is IrClass if declaration.isCompanion && staticFieldsToInitializers.isNotEmpty() -> {
                        // Special handling of companion objects - if the static_init function is introduced, the Companion_getInstance
                        // body should be moved to the static_init body to preserve the correct order of initialization.
                        // _getInstance then calls static_init instead.
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
                    }
                }
            }
        }

        container.staticInitializerProcessed = true
        if (initializers.isEmpty() && container.superClass?.staticInitializer == null) return

        // It is important to define stable signature via restrictTo to be able to reference static_init of super class
        // defined in a separate module.
        val [staticInitCalledField, staticInitFunction] = context.irFactory.stageController.restrictTo(container) {
            val initCalledField = createStaticInitCalledField(container)
            val initFunction = createInitFunction(
                container = container,
                origin = STATIC_CLASS_INITIALIZER,
                initCalledVar = initCalledField,
                initializers = initializers
            )
            initCalledField to initFunction
        }

        for (declaration in container.declarations) {
            when (declaration) {
                is IrEnumEntry -> {
                    declaration.getInstanceFun?.let { getInstance ->
                        val body = getInstance.body as? IrBlockBody
                        staticInitFunction.let { body?.statements?.add(0, builder.irCall(it.symbol)) }
                    }
                }
                is IrSimpleFunction, is IrConstructor -> {
                    if (declaration.dispatchReceiverParameter != null) continue // already initialized when instance was created
                    val body = declaration.body as? IrBlockBody ?: continue
                    staticInitFunction.let { body.statements.add(0, builder.irCall(it.symbol)) }
                }
            }
        }

        // Adding static_init declaration after adding its usages to make sure we don't insert usages inside static_init itself
        container.staticInitializer = staticInitFunction
        container.declarations.addAll(0, listOf(staticInitCalledField, staticInitFunction))
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

    private fun createInitFunction(
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

                container.superClass?.staticInitializer?.let {
                    statements += builder.irCall(it.symbol)
                }

                for (initializer in initializers) {
                    initializer.setDeclarationsParent(initFunction)
                }
                statements += initializers
            }
        }
    }
}
