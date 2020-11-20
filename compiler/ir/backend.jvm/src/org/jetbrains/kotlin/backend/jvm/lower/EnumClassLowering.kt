/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import gnu.trove.TObjectIntHashMap
import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.lower.at
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.ir.createJvmIrBuilder
import org.jetbrains.kotlin.backend.jvm.ir.irArray
import org.jetbrains.kotlin.codegen.ImplementationBodyCodegen
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.addField
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildConstructor
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrExpressionBodyImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrSetValueImpl
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueParameterSymbol
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.isEnumClass
import org.jetbrains.kotlin.ir.util.isEnumEntry
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

internal val enumClassPhase = makeIrFilePhase(
    ::EnumClassLowering,
    name = "EnumClass",
    description = "Handle enum classes"
)

private class EnumClassLowering(val context: JvmBackendContext) : ClassLoweringPass {
    override fun lower(irClass: IrClass) {
        if (!irClass.isEnumClass) return
        EnumClassTransformer(irClass).run()
    }

    private inner class EnumClassTransformer(val irClass: IrClass) {
        private val loweredEnumConstructors = hashMapOf<IrConstructorSymbol, IrConstructor>()
        private val loweredEnumConstructorParameters = hashMapOf<IrValueParameterSymbol, IrValueParameter>()
        private val enumEntryOrdinals = TObjectIntHashMap<IrEnumEntry>()
        private val declarationToEnumEntry = mutableMapOf<IrDeclaration, IrEnumEntry>()

        fun run() {
            // Lower IrEnumEntry into IrField and IrClass members
            irClass.declarations.asSequence().filterIsInstance<IrEnumEntry>().withIndex().forEach { (index, enumEntry) ->
                enumEntryOrdinals.put(enumEntry, index)
                enumEntry.correspondingClass?.let { entryClass -> declarationToEnumEntry[entryClass] = enumEntry }
                declarationToEnumEntry[buildEnumEntryField(enumEntry)] = enumEntry
            }

            irClass.declarations.removeAll { it is IrEnumEntry }
            irClass.declarations += declarationToEnumEntry.keys

            // Construct the synthetic $VALUES field, which contains an array of all enum entries
            val valuesField = buildValuesField()

            // Add synthetic parameters to enum constructors and implement the values and valueOf functions
            irClass.transformChildrenVoid(EnumClassDeclarationsTransformer(valuesField))

            // Add synthetic arguments to enum constructor calls and remap enum constructor paramters
            irClass.transformChildrenVoid(EnumClassCallTransformer())
        }

        private fun buildEnumEntryField(enumEntry: IrEnumEntry): IrField =
            context.cachedDeclarations.getFieldForEnumEntry(enumEntry).apply {
                initializer = IrExpressionBodyImpl(enumEntry.initializerExpression!!.expression.patchDeclarationParents(this))
                annotations += enumEntry.annotations
            }

        private fun buildValuesField(): IrField = irClass.addField {
            name = Name.identifier(ImplementationBodyCodegen.ENUM_VALUES_FIELD_NAME)
            type = context.irBuiltIns.arrayClass.typeWith(irClass.defaultType)
            visibility = DescriptorVisibilities.PRIVATE
            origin = IrDeclarationOrigin.FIELD_FOR_ENUM_VALUES
            isFinal = true
            isStatic = true
        }.apply {
            initializer = context.createJvmIrBuilder(symbol).run {
                irExprBody(irArray(type) {
                    for (irField in declarationToEnumEntry.keys.filterIsInstance<IrField>()) {
                        +irGetField(null, irField)
                    }
                })
            }
        }

        private inner class EnumClassDeclarationsTransformer(val valuesField: IrField) : IrElementTransformerVoid() {
            override fun visitClass(declaration: IrClass): IrStatement =
                if (declaration.isEnumEntry) super.visitClass(declaration) else declaration

            override fun visitConstructor(declaration: IrConstructor): IrStatement =
                context.irFactory.buildConstructor {
                    updateFrom(declaration)
                    returnType = declaration.returnType
                }.apply {
                    parent = declaration.parent

                    addValueParameter(
                        "\$enum\$name", context.irBuiltIns.stringType, JvmLoweredDeclarationOrigin.ENUM_CONSTRUCTOR_SYNTHETIC_PARAMETER
                    )
                    addValueParameter(
                        "\$enum\$ordinal", context.irBuiltIns.intType, JvmLoweredDeclarationOrigin.ENUM_CONSTRUCTOR_SYNTHETIC_PARAMETER
                    )
                    valueParameters += declaration.valueParameters.map { param ->
                        param.copyTo(this, index = param.index + 2).also { newParam ->
                            loweredEnumConstructorParameters[param.symbol] = newParam
                        }
                    }

                    body = declaration.body?.patchDeclarationParents(this)
                    loweredEnumConstructors[declaration.symbol] = this
                    metadata = declaration.metadata
                }

            override fun visitSimpleFunction(declaration: IrSimpleFunction): IrStatement {
                val body = declaration.body?.safeAs<IrSyntheticBody>()
                    ?: return declaration

                declaration.body = context.createJvmIrBuilder(declaration.symbol).run {
                    irExprBody(
                        when (body.kind) {
                            IrSyntheticBodyKind.ENUM_VALUES ->
                                irArray(valuesField.type) { addSpread(irGetField(null, valuesField)) }

                            IrSyntheticBodyKind.ENUM_VALUEOF ->
                                irCall(backendContext.ir.symbols.enumValueOfFunction).apply {
                                    putValueArgument(0, with(FunctionReferenceLowering) {
                                        javaClassReference(irClass.defaultType, backendContext)
                                    })
                                    putValueArgument(1, irGet(declaration.valueParameters[0]))
                                }
                        }
                    )
                }
                return declaration
            }
        }

        private inner class EnumClassCallTransformer : IrElementTransformerVoidWithContext() {
            override fun visitClassNew(declaration: IrClass): IrStatement =
                if (declaration.isEnumEntry) super.visitClassNew(declaration) else declaration

            override fun visitGetValue(expression: IrGetValue): IrExpression =
                loweredEnumConstructorParameters[expression.symbol]?.let {
                    IrGetValueImpl(expression.startOffset, expression.endOffset, it.type, it.symbol, expression.origin)
                } ?: expression

            override fun visitSetValue(expression: IrSetValue): IrExpression {
                expression.transformChildrenVoid()
                return loweredEnumConstructorParameters[expression.symbol]?.let {
                    IrSetValueImpl(expression.startOffset, expression.endOffset, it.type, it.symbol, expression.value, expression.origin)
                } ?: expression
            }

            override fun visitEnumConstructorCall(expression: IrEnumConstructorCall): IrExpression {
                expression.transformChildrenVoid(this)
                val scopeOwnerSymbol = currentScope!!.scope.scopeOwnerSymbol
                return context.createIrBuilder(scopeOwnerSymbol).at(expression).run {
                    val constructor = loweredEnumConstructors[expression.symbol] ?: expression.symbol.owner

                    if (scopeOwnerSymbol is IrConstructorSymbol) {
                        irDelegatingConstructorCall(constructor)
                    } else {
                        irCall(constructor)
                    }.also {
                        passConstructorArguments(it, expression, declarationToEnumEntry[scopeOwnerSymbol.owner as IrDeclaration])
                    }
                }
            }

            override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall): IrExpression {
                expression.transformChildrenVoid(this)
                val replacement = loweredEnumConstructors[expression.symbol]
                    ?: return expression

                return context.createIrBuilder(currentScope!!.scope.scopeOwnerSymbol).at(expression).run {
                    irDelegatingConstructorCall(replacement).also { passConstructorArguments(it, expression) }
                }
            }

            private fun IrBuilderWithScope.passConstructorArguments(
                call: IrFunctionAccessExpression,
                original: IrFunctionAccessExpression,
                enumEntry: IrEnumEntry? = null
            ) {
                call.copyTypeArgumentsFrom(original)
                if (enumEntry != null) {
                    call.putValueArgument(0, irString(enumEntry.name.asString()))
                    call.putValueArgument(1, irInt(enumEntryOrdinals[enumEntry]))
                } else {
                    val constructor = currentScope!!.scope.scopeOwnerSymbol as IrConstructorSymbol
                    call.putValueArgument(0, irGet(constructor.owner.valueParameters[0]))
                    call.putValueArgument(1, irGet(constructor.owner.valueParameters[1]))
                }
                for (index in 0 until original.valueArgumentsCount) {
                    original.getValueArgument(index)?.let { call.putValueArgument(index + 2, it) }
                }
            }
        }
    }
}
