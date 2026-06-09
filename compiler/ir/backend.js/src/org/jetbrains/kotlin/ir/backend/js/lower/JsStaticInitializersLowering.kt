/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlockBody
import org.jetbrains.kotlin.backend.common.lower.irIfThen
import org.jetbrains.kotlin.backend.common.phaser.PhasePrerequisites
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities.PRIVATE
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.initEntryInstancesFun
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.objectGetInstanceFunction
import org.jetbrains.kotlin.ir.backend.js.staticInitializer
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
    EnumClassCreateInitializerLowering::class,
    EnumEntryCreateGetInstancesFunsLowering::class,
)
internal class JsStaticInitializersLowering(private val context: JsIrBackendContext) : FileLoweringPass {
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

    private val visited = hashSetOf<IrClass>()
    private fun processDeclarationContainer(container: IrClass) {
        if (container in visited) return
        if (container.isEffectivelyExternal()) return

        // Before building child static_init, we need to be sure that all super classes are processed in order before the child.
        // This is needed for 2 reasons:
        // 1. To create a call to a parent static_init in the child static_init body.
        // 2. To create child static_init even if the child doesn't have any initializers, but super class has.
        container.superClass?.let { processDeclarationContainer(it) }

        val builder = context.irBuiltIns.createIrBuilder(container.symbol, SYNTHETIC_OFFSET, SYNTHETIC_OFFSET)
        val staticDeclarationsByFields = buildMap {
            for (declaration in container.declarations) {
                val field = declaration as? IrField ?: (declaration as? IrProperty)?.backingField ?: continue
                if (!field.isStatic) continue
                val initializer = field.initializer?.expression ?: continue
                put(declaration, field to initializer)
            }
        }

        val initializers = buildList {
            // Enum cases initializers always go first.
            // Call _initEntries in static_init instead of get_instance, to initialize entries when static members got accessed.
            container.initEntryInstancesFun?.let { initEntries ->
                add(builder.irCall(initEntries.symbol))
            }

            for (declaration in container.declarations) {
                when (declaration) {
                    in staticDeclarationsByFields -> {
                        staticDeclarationsByFields[declaration]?.let { [field, initializer] ->
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
                    is IrClass if declaration.isCompanion && staticDeclarationsByFields.isNotEmpty() -> {
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

        visited += container
        if (initializers.isEmpty() && container.superClass?.staticInitializer == null) return

        val staticInitCalledField = createStaticInitCalledField(container)
        val staticInitFunction = createInitFunction(
            container = container,
            origin = STATIC_CLASS_INITIALIZER,
            initCalledVar = staticInitCalledField,
            initializers = initializers
        )

        for (declaration in container.declarations.asSequence().filterIsInstance<IrFunction>()) {
            if (declaration is IrSimpleFunction || declaration is IrConstructor) {
                if (declaration.dispatchReceiverParameter != null) continue // already initialized when instance was created
                val body = declaration.body as? IrBlockBody ?: continue
                staticInitFunction.let { body.statements.add(0, builder.irCall(it.symbol)) }
            }
        }

        container.initEntryInstancesFun?.let { initEntries ->
            // Replace the call to a _initEntries function to a static_init call. This ensures touching enum entries will trigger
            // static initializers too.
            container.parent.transformChildrenVoid(object : IrElementTransformerVoidWithContext() {
                override fun visitCall(expression: IrCall): IrExpression {
                    val enclosingFunction = currentFunction?.irElement as? IrSimpleFunction ?: return super.visitCall(expression)
                    if (expression.symbol == initEntries.symbol && enclosingFunction != staticInitFunction)
                        return builder.irCall(staticInitFunction.symbol)
                    return super.visitCall(expression)
                }
            })
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
