/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.BackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.ir.ExpectSymbolTransformer
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.extractTypeParameters
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.multiplatform.OptionalAnnotationUtil
import org.jetbrains.kotlin.resolve.multiplatform.findCompatibleActualsForExpected
import org.jetbrains.kotlin.resolve.multiplatform.findCompatibleExpectsForActual
import kotlin.collections.set

// `doRemove` means should expect-declaration be removed from IR
@OptIn(ObsoleteDescriptorBasedAPI::class)
open class ExpectDeclarationRemover(val symbolTable: ReferenceSymbolTable, private val doRemove: Boolean)
    : ExpectSymbolTransformer(), FileLoweringPass {

    constructor(context: BackendContext) : this(context.ir.symbols.externalSymbolTable, true)

    private val typeParameterSubstitutionMap = mutableMapOf<Pair<IrFunction, IrFunction>, Map<IrTypeParameter, IrTypeParameter>>()

    override fun lower(irFile: IrFile) {
        visitFile(irFile)
    }

    override fun visitFile(declaration: IrFile): IrFile {
        if (doRemove) {
            declaration.declarations.removeAll { shouldRemoveTopLevelDeclaration(it) }
        }
        return super.visitFile(declaration)
    }

    override fun visitValueParameter(declaration: IrValueParameter): IrStatement {
        tryCopyDefaultArguments(declaration)
        return super.visitValueParameter(declaration)
    }

    fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        if (declaration.isTopLevelDeclaration && shouldRemoveTopLevelDeclaration(declaration)) {
            return emptyList()
        }

        if (declaration is IrValueParameter) {
            tryCopyDefaultArguments(declaration)
        }

        return null
    }

    override fun getActualClass(descriptor: ClassDescriptor): IrClassSymbol? {
        return symbolTable.referenceClass(
            descriptor.findActualForExpect() as? ClassDescriptor ?: return null
        )
    }

    override fun getActualProperty(descriptor: PropertyDescriptor): ActualPropertyResult? {
        val newSymbol = symbolTable.referenceProperty(
            descriptor.findActualForExpect() as? PropertyDescriptor ?: return null
        )
        val newGetter = newSymbol.descriptor.getter?.let { symbolTable.referenceSimpleFunction(it) }
        val newSetter = newSymbol.descriptor.setter?.let { symbolTable.referenceSimpleFunction(it) }
        return ActualPropertyResult(newSymbol, newGetter, newSetter)
    }

    override fun getActualConstructor(descriptor: ClassConstructorDescriptor): IrConstructorSymbol? {
        return symbolTable.referenceConstructor(
            descriptor.findActualForExpect() as? ClassConstructorDescriptor ?: return null
        )
    }

    override fun getActualFunction(descriptor: FunctionDescriptor): IrSimpleFunctionSymbol? {
        return symbolTable.referenceSimpleFunction(
            descriptor.findActualForExpect() as? FunctionDescriptor ?: return null
        )
    }

    private fun MemberDescriptor.findActualForExpect(): MemberDescriptor? {
        if (!isExpect) error(this)
        return findCompatibleActualsForExpected(module).singleOrNull()
    }

    private fun shouldRemoveTopLevelDeclaration(declaration: IrDeclaration): Boolean {
        return doRemove && when (declaration) {
            is IrClass -> declaration.isExpect
            is IrProperty -> declaration.isExpect
            is IrFunction -> declaration.isExpect
            else -> false
        }
    }

    private fun isOptionalAnnotationClass(klass: IrClass): Boolean {
        return klass.kind == ClassKind.ANNOTATION_CLASS &&
                klass.isExpect &&
                klass.annotations.hasAnnotation(OptionalAnnotationUtil.OPTIONAL_EXPECTATION_FQ_NAME)
    }

    private fun tryCopyDefaultArguments(declaration: IrValueParameter) {
        // Keep actual default value if present. They are generally not allowed but can be suppressed with
        // @Suppress("ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS")
        if (declaration.defaultValue != null) {
            return
        }

        val function = declaration.parent as? IrFunction ?: return

        if (function is IrConstructor) {
            if (isOptionalAnnotationClass(function.constructedClass)) {
                return
            }
        }

        if (!function.descriptor.isActual) return

        val index = declaration.index

        if (index < 0) return

        assert(function.valueParameters[index] == declaration)

        // If the containing declaration is an `expect class` that matches an `actual typealias`,
        // the `actual fun` or `actual constructor` for this may be in a different module.
        // Nothing we can do with those.
        // TODO they may not actually have the defaults though -- may be a frontend bug.
        val expectFunction =
            (function.descriptor.findExpectForActual() as? FunctionDescriptor)?.let { symbolTable.referenceFunction(it).owner }
                ?: return

        val defaultValue = expectFunction.valueParameters[index].defaultValue ?: return

        val expectToActual = expectFunction to function
        if (expectToActual !in typeParameterSubstitutionMap) {
            val functionTypeParameters = extractTypeParameters(function)
            val expectFunctionTypeParameters = extractTypeParameters(expectFunction)

            expectFunctionTypeParameters.zip(functionTypeParameters).let { typeParametersMapping ->
                typeParameterSubstitutionMap[expectToActual] = typeParametersMapping.toMap()
            }
        }

        defaultValue.let { originalDefault ->
            declaration.defaultValue = originalDefault.copyAndActualizeDefaultValue(
                function,
                typeParameterSubstitutionMap.getValue(expectToActual)
            )
        }
    }

    private fun IrExpressionBody.copyAndActualizeDefaultValue(
        actualFunction: IrFunction,
        expectActualTypeParametersMap: Map<IrTypeParameter, IrTypeParameter>
    ): IrExpressionBody {
        return this
            .deepCopyWithSymbols(actualFunction) { symbolRemapper, _ ->
                DeepCopyIrTreeWithSymbols(symbolRemapper, IrTypeParameterRemapper(expectActualTypeParametersMap))
            }
            .transform(object : IrElementTransformerVoid() {
                override fun visitGetValue(expression: IrGetValue): IrExpression {
                    expression.transformChildrenVoid()
                    return expression.actualize(
                        classActualizer = { symbolTable.referenceClass(it.descriptor.findActualForExpect() as ClassDescriptor).owner },
                        functionActualizer = { symbolTable.referenceFunction(it.descriptor.findActualForExpect() as FunctionDescriptor).owner }
                    )
                }
            }, data = null)
    }

    private fun MemberDescriptor.findExpectForActual(): MemberDescriptor? {
        if (!isActual) error(this)
        return findCompatibleExpectsForActual().singleOrNull()
    }
}
