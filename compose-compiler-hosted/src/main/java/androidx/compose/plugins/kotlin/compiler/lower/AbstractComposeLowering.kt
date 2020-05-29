/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.plugins.kotlin.compiler.lower

import androidx.compose.plugins.kotlin.ComposableAnnotationChecker
import androidx.compose.plugins.kotlin.ComposeFqNames
import androidx.compose.plugins.kotlin.KtxNameConventions
import androidx.compose.plugins.kotlin.analysis.ComposeWritableSlices
import androidx.compose.plugins.kotlin.irTrace
import androidx.compose.plugins.kotlin.isEmitInline
import androidx.compose.plugins.kotlin.isMarkedStable
import androidx.compose.plugins.kotlin.isSpecialType
import org.jetbrains.kotlin.backend.common.descriptors.isFunctionOrKFunctionType
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.jvm.lower.inlineclasses.InlineClassAbi
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.extractParameterNameFromFunctionTypeArgument
import org.jetbrains.kotlin.builtins.getReceiverTypeFromFunctionType
import org.jetbrains.kotlin.builtins.getReturnTypeFromFunctionType
import org.jetbrains.kotlin.builtins.getValueParameterTypesFromFunctionType
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.utils.OperatorNames
import org.jetbrains.kotlin.ir.builders.IrBlockBodyBuilder
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueDeclaration
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrTypeParameterImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.descriptors.IrTemporaryVariableDescriptorImpl
import org.jetbrains.kotlin.ir.expressions.IrBranch
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrBranchImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrElseBranchImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrIfThenElseImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrSetVariableImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrWhenImpl
import org.jetbrains.kotlin.ir.expressions.typeParametersCount
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrStatementOriginImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrWhileLoopImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrReturnTargetSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrTypeParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.isNullable
import org.jetbrains.kotlin.ir.types.isPrimitiveType
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.util.ConstantValueGenerator
import org.jetbrains.kotlin.ir.util.DeepCopySymbolRemapper
import org.jetbrains.kotlin.ir.util.TypeTranslator
import org.jetbrains.kotlin.ir.util.endOffset
import org.jetbrains.kotlin.ir.util.getPrimitiveArrayElementType
import org.jetbrains.kotlin.ir.util.getSimpleFunction
import org.jetbrains.kotlin.ir.util.referenceFunction
import org.jetbrains.kotlin.ir.util.startOffset
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.resolve.DescriptorFactory
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import org.jetbrains.kotlin.resolve.isInlineClassType
import org.jetbrains.kotlin.resolve.unsubstitutedUnderlyingType
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.isError
import org.jetbrains.kotlin.types.isNullable
import org.jetbrains.kotlin.types.typeUtil.isTypeParameter
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult

abstract class AbstractComposeLowering(
    val context: IrPluginContext,
    val symbolRemapper: DeepCopySymbolRemapper,
    val bindingTrace: BindingTrace
) : IrElementTransformerVoid() {

    protected val typeTranslator =
        TypeTranslator(
            context.symbolTable,
            context.languageVersionSettings,
            context.builtIns
        ).apply {
            constantValueGenerator = ConstantValueGenerator(
                context.moduleDescriptor,
                context.symbolTable
            )
            constantValueGenerator.typeTranslator = this
        }

    protected val builtIns = context.irBuiltIns

    protected val composerTypeDescriptor = context.moduleDescriptor
        .findClassAcrossModuleDependencies(ClassId.topLevel(ComposeFqNames.Composer)
    ) ?: error("Cannot find the Composer class")

    private val symbolTable get() = context.symbolTable

    fun referenceFunction(descriptor: CallableDescriptor): IrFunctionSymbol {
        return symbolRemapper.getReferencedFunction(symbolTable.referenceFunction(descriptor))
    }

    fun referenceSimpleFunction(descriptor: SimpleFunctionDescriptor): IrSimpleFunctionSymbol {
        return symbolRemapper.getReferencedSimpleFunction(
            symbolTable.referenceSimpleFunction(descriptor)
        )
    }

    fun referenceConstructor(descriptor: ClassConstructorDescriptor): IrConstructorSymbol {
        return symbolRemapper.getReferencedConstructor(symbolTable.referenceConstructor(descriptor))
    }

    fun getTopLevelClass(fqName: FqName): IrClassSymbol {
        val descriptor = context.moduleDescriptor.getPackage(fqName.parent()).memberScope
            .getContributedClassifier(
                fqName.shortName(), NoLookupLocation.FROM_BACKEND
            ) as ClassDescriptor? ?: error("Class is not found: $fqName")
        return symbolTable.referenceClass(descriptor)
    }

    fun getTopLevelFunction(fqName: FqName): IrFunctionSymbol {
        val descriptor = context.moduleDescriptor.getPackage(fqName.parent()).memberScope
            .getContributedFunctions(
                fqName.shortName(), NoLookupLocation.FROM_BACKEND
            ).singleOrNull() ?: error("Function not found $fqName")
        return symbolRemapper.getReferencedFunction(
            symbolTable.referenceSimpleFunction(descriptor)
        )
    }

    fun getTopLevelPropertyGetter(fqName: FqName): IrFunctionSymbol {
        val descriptor = context.moduleDescriptor.getPackage(fqName.parent()).memberScope
            .getContributedVariables(
                fqName.shortName(), NoLookupLocation.FROM_BACKEND
            ).singleOrNull() ?: error("Function not found $fqName")
        return symbolRemapper.getReferencedFunction(
            symbolTable.referenceSimpleFunction(descriptor.getter!!)
        )
    }

    fun KotlinType.toIrType(): IrType = typeTranslator.translateType(this)

    fun IrType.unboxInlineClass() = unboxType() ?: this

    fun <T : IrSymbol> T.bindIfNecessary(): T {
        if (!isBound) {
            context.irProviders.firstNotNullResult { it.getDeclaration(this) }
        }
        return this
    }

    // NOTE(lmr): This implementation mimics the kotlin-provided unboxInlineClass method, except
    // this one makes sure to bind the symbol if it is unbound, so is a bit safer to use.
    fun IrType.unboxType(): IrType? {
        val classSymbol = classOrNull ?: return null
        val klass = classSymbol.bindIfNecessary().owner
        if (!klass.isInline) return null

        // TODO: Apply type substitutions
        val underlyingType = InlineClassAbi.getUnderlyingType(klass).unboxInlineClass()
        if (!isNullable()) return underlyingType
        if (underlyingType.isNullable() || underlyingType.isPrimitiveType())
            return null
        return underlyingType.makeNullable()
    }

    fun IrAnnotationContainer.hasComposableAnnotation(): Boolean {
        return annotations.hasAnnotation(ComposeFqNames.Composable)
    }

    fun IrAnnotationContainer.hasStableAnnotation(): Boolean {
        return annotations.hasAnnotation(ComposeFqNames.Stable)
    }

    fun List<IrConstructorCall>.hasAnnotation(fqName: FqName): Boolean =
        any { it.symbol.descriptor.constructedClass.fqNameSafe == fqName }

    fun IrCall.isTransformedComposableCall(): Boolean {
        return context.irTrace[ComposeWritableSlices.IS_COMPOSABLE_CALL, this] ?: false
    }

    fun IrCall.isSyntheticComposableCall(): Boolean {
        return context.irTrace[ComposeWritableSlices.IS_SYNTHETIC_COMPOSABLE_CALL, this] == true
    }

    fun IrFunction.isInlinedLambda(): Boolean {
        descriptor.findPsi()?.let { psi ->
            (psi as? KtFunctionLiteral)?.let {
                if (InlineUtil.isInlinedArgument(
                        it,
                        context.bindingContext,
                        false
                    )
                )
                    return true
                if (it.isEmitInline(context.bindingContext)) {
                    return true
                }
            }
        }
        return false
    }

    private val composableChecker = ComposableAnnotationChecker()

    protected val KotlinType.isEnum
        get() =
            (constructor.declarationDescriptor as? ClassDescriptor)?.kind == ClassKind.ENUM_CLASS

    protected fun KotlinType?.isStable(): Boolean {
        if (this == null) return false

        val trace = bindingTrace
        val calculated = trace.get(ComposeWritableSlices.STABLE_TYPE, this)
        return if (calculated == null) {
            val isStable = !isError &&
                    !isTypeParameter() &&
                    !isSpecialType &&
                    (
                            KotlinBuiltIns.isPrimitiveType(this) ||
                                    isFunctionOrKFunctionType ||
                                    isEnum ||
                                    KotlinBuiltIns.isString(this) ||
                                    isMarkedStable() ||
                                    (
                                            isNullable() &&
                                                    makeNotNullable().isStable()
                                            ) ||
                                    (
                                            isInlineClassType() &&
                                                    unsubstitutedUnderlyingType().isStable()
                                            )
                            )
            trace.record(ComposeWritableSlices.STABLE_TYPE, this, isStable)
            isStable
        } else calculated
    }

    fun FunctionDescriptor.isComposable(): Boolean {
        val composability = composableChecker.analyze(bindingTrace, this)
        return when (composability) {
            ComposableAnnotationChecker.Composability.NOT_COMPOSABLE -> false
            ComposableAnnotationChecker.Composability.MARKED -> true
            ComposableAnnotationChecker.Composability.INFERRED -> true
        }
    }

    fun IrFunction.isComposable(): Boolean = descriptor.isComposable()
    fun IrFunctionExpression.isComposable(): Boolean = function.isComposable()

    private fun IrFunction.createParameterDeclarations() {
        fun ParameterDescriptor.irValueParameter() = IrValueParameterImpl(
            this.startOffset ?: UNDEFINED_OFFSET,
            this.endOffset ?: UNDEFINED_OFFSET,
            IrDeclarationOrigin.DEFINED,
            this,
            type.toIrType(),
            (this as? ValueParameterDescriptor)?.varargElementType?.toIrType()
        ).also {
            it.parent = this@createParameterDeclarations
        }

        fun TypeParameterDescriptor.irTypeParameter() = IrTypeParameterImpl(
            this.startOffset ?: UNDEFINED_OFFSET,
            this.endOffset ?: UNDEFINED_OFFSET,
            IrDeclarationOrigin.DEFINED,
            IrTypeParameterSymbolImpl(this)
        ).also {
            it.parent = this@createParameterDeclarations
        }

        dispatchReceiverParameter = descriptor.dispatchReceiverParameter?.irValueParameter()
        extensionReceiverParameter = descriptor.extensionReceiverParameter?.irValueParameter()

        assert(valueParameters.isEmpty())
        descriptor.valueParameters.mapTo(valueParameters) { it.irValueParameter() }

        assert(typeParameters.isEmpty())
        descriptor.typeParameters.mapTo(typeParameters) { it.irTypeParameter() }
    }

    protected fun IrBuilderWithScope.irLambdaExpression(
        descriptor: FunctionDescriptor,
        type: IrType,
        body: IrBlockBodyBuilder.(IrFunction) -> Unit
    ) = irLambdaExpression(this.startOffset, this.endOffset, descriptor, type, body)

    protected fun IrBuilderWithScope.irLambdaExpression(
        startOffset: Int,
        endOffset: Int,
        descriptor: FunctionDescriptor,
        type: IrType,
        body: IrBlockBodyBuilder.(IrFunction) -> Unit
    ): IrExpression {
        val symbol = IrSimpleFunctionSymbolImpl(descriptor)

        val returnType = descriptor.returnType!!.toIrType()

        val lambda = IrFunctionImpl(
            startOffset, endOffset,
            IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA,
            symbol,
            returnType
        ).also {
            it.parent = scope.getLocalDeclarationParent()
            it.createParameterDeclarations()
            it.body = DeclarationIrBuilder(this@AbstractComposeLowering.context, symbol)
                .irBlockBody { body(it) }
        }

        return irBlock(
            startOffset = startOffset,
            endOffset = endOffset,
            origin = IrStatementOrigin.LAMBDA,
            resultType = type
        ) {
            +lambda
            +IrFunctionReferenceImpl(
                startOffset = startOffset,
                endOffset = endOffset,
                type = type,
                symbol = symbol,
                typeArgumentsCount = descriptor.typeParametersCount,
                origin = IrStatementOrigin.LAMBDA
            )
        }
    }

    protected fun IrBuilderWithScope.createFunctionDescriptor(
        type: KotlinType,
        owner: DeclarationDescriptor = scope.scopeOwner
    ): FunctionDescriptor {
        return AnonymousFunctionDescriptor(
            owner,
            Annotations.EMPTY,
            CallableMemberDescriptor.Kind.SYNTHESIZED,
            SourceElement.NO_SOURCE,
            false
        ).apply {
            initialize(
                type.getReceiverTypeFromFunctionType()?.let {
                    DescriptorFactory.createExtensionReceiverParameterForCallable(
                        this,
                        it,
                        Annotations.EMPTY
                    )
                },
                null,
                emptyList(),
                type.getValueParameterTypesFromFunctionType().mapIndexed { i, t ->
                    ValueParameterDescriptorImpl(
                        containingDeclaration = this,
                        original = null,
                        index = i,
                        annotations = Annotations.EMPTY,
                        name = t.type.extractParameterNameFromFunctionTypeArgument()
                            ?: Name.identifier("p$i"),
                        outType = t.type,
                        declaresDefaultValue = false,
                        isCrossinline = false,
                        isNoinline = false,
                        varargElementType = null,
                        source = SourceElement.NO_SOURCE
                    )
                },
                type.getReturnTypeFromFunctionType(),
                Modality.FINAL,
                Visibilities.LOCAL,
                null
            )
            isOperator = false
            isInfix = false
            isExternal = false
            isInline = false
            isTailrec = false
            isSuspend = false
            isExpect = false
            isActual = false
        }
    }

    // set the bit at a certain index
    protected fun Int.withBit(index: Int, value: Boolean): Int {
        return if (value) {
            this or (1 shl index)
        } else {
            this and (1 shl index).inv()
        }
    }

    protected operator fun Int.get(index: Int): Boolean {
        return this and (1 shl index) != 0
    }

    // create a bitmask with the following bits
    protected fun bitMask(vararg values: Boolean): Int = values.foldIndexed(0) { i, mask, bit ->
        mask.withBit(i, bit)
    }

    protected fun irGetBit(param: IrDefaultBitMaskValue, index: Int): IrExpression {
        // value and (1 shl index) != 0
        return irNotEqual(
            param.irIsolateBitAtIndex(index),
            irConst(0)
        )
    }

    protected fun irSet(variable: IrVariable, value: IrExpression): IrExpression {
        return IrSetVariableImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            context.irBuiltIns.unitType,
            variable.symbol,
            value = value,
            origin = null
        )
    }

    protected fun irCall(
        symbol: IrFunctionSymbol,
        origin: IrStatementOrigin? = null,
        dispatchReceiver: IrExpression? = null,
        extensionReceiver: IrExpression? = null,
        vararg args: IrExpression
    ): IrCallImpl {
        symbol.bindIfNecessary()
        return IrCallImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            symbol.owner.returnType,
            symbol,
            origin
        ).also {
            if (dispatchReceiver != null) it.dispatchReceiver = dispatchReceiver
            if (extensionReceiver != null) it.extensionReceiver = extensionReceiver
            args.forEachIndexed { index, arg ->
                it.putValueArgument(index, arg)
            }
        }
    }

    protected fun irAnd(lhs: IrExpression, rhs: IrExpression): IrCallImpl {
        return irCall(
            context.symbols.intAnd,
            null,
            lhs,
            null,
            rhs
        )
    }

    protected fun irInv(lhs: IrExpression): IrCallImpl {
        val int = context.builtIns.intType
        return irCall(
            context.symbols.getUnaryOperator(OperatorNames.INV, int),
            null,
            lhs
        )
    }

    protected fun irOr(lhs: IrExpression, rhs: IrExpression): IrCallImpl {
        val int = context.builtIns.intType
        return irCall(
            context.symbols.getBinaryOperator(OperatorNames.OR, int, int),
            null,
            lhs,
            null,
            rhs
        )
    }

    protected fun irOrOr(lhs: IrExpression, rhs: IrExpression): IrExpression {
        return IrWhenImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            origin = IrStatementOrigin.OROR,
            type = context.irBuiltIns.booleanType,
            branches = listOf(
                IrBranchImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    condition = lhs,
                    result = irConst(true)
                ),
                IrElseBranchImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    condition = irConst(true),
                    result = rhs
                )
            )
        )
    }

    protected fun irAndAnd(lhs: IrExpression, rhs: IrExpression): IrExpression {
        return IrWhenImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            origin = IrStatementOrigin.ANDAND,
            type = context.irBuiltIns.booleanType,
            branches = listOf(
                IrBranchImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    condition = lhs,
                    result = rhs
                ),
                IrElseBranchImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    condition = irConst(true),
                    result = irConst(false)
                )
            )
        )
    }

    protected fun irXor(lhs: IrExpression, rhs: IrExpression): IrCallImpl {
        val int = context.builtIns.intType
        return irCall(
            context.symbols.getBinaryOperator(OperatorNames.XOR, int, int),
            null,
            lhs,
            null,
            rhs
        )
    }

    protected fun irReturn(
        target: IrReturnTargetSymbol,
        value: IrExpression,
        type: IrType = value.type
    ): IrExpression {
        return IrReturnImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            type,
            target,
            value
        )
    }

    protected fun irEqual(lhs: IrExpression, rhs: IrExpression): IrExpression {
        return irCall(
            context.irBuiltIns.eqeqeqSymbol,
            null,
            null,
            null,
            lhs,
            rhs
        )
    }

    protected fun irNot(value: IrExpression): IrExpression {
        return irCall(
            context.irBuiltIns.booleanNotSymbol,
            dispatchReceiver = value
        )
    }

    protected fun irNotEqual(lhs: IrExpression, rhs: IrExpression): IrExpression {
        return irNot(irEqual(lhs, rhs))
    }

//        context.irIntrinsics.symbols.intAnd
//        context.irIntrinsics.symbols.getBinaryOperator(name, lhs, rhs)
//        context.irBuiltIns.booleanNotSymbol
//        context.irBuiltIns.eqeqeqSymbol
//        context.irBuiltIns.eqeqSymbol
//        context.irBuiltIns.greaterFunByOperandType

    protected fun irConst(value: Int): IrConst<Int> = IrConstImpl(
        UNDEFINED_OFFSET,
        UNDEFINED_OFFSET,
        context.irBuiltIns.intType,
        IrConstKind.Int,
        value
    )

    protected fun irConst(value: Boolean) = IrConstImpl(
        UNDEFINED_OFFSET,
        UNDEFINED_OFFSET,
        context.irBuiltIns.booleanType,
        IrConstKind.Boolean,
        value
    )

    protected fun irNull() = IrConstImpl(
        UNDEFINED_OFFSET,
        UNDEFINED_OFFSET,
        context.irBuiltIns.anyNType,
        IrConstKind.Null,
        null
    )

    protected fun irForLoop(
        scope: DeclarationDescriptor,
        elementType: IrType,
        subject: IrExpression,
        loopBody: (IrValueDeclaration) -> IrExpression
    ): IrStatement {
        val primitiveType = subject.type.getPrimitiveArrayElementType()
        val iteratorSymbol = primitiveType?.let {
            context.symbols.primitiveIteratorsByType[it]
        } ?: context.symbols.iterator
        val unitType = context.irBuiltIns.unitType

        val getIteratorSymbol = subject.type.classOrNull!!.getSimpleFunction("iterator")!!
        val nextSymbol = iteratorSymbol.getSimpleFunction("next")!!
        val hasNextSymbol = iteratorSymbol.getSimpleFunction("hasNext")!!

        val iteratorVar = irTemporary(
            containingDeclaration = scope,
            value = irCall(
                symbol = getIteratorSymbol,
                origin = IrStatementOrigin.FOR_LOOP_ITERATOR,
                dispatchReceiver = subject
            ),
            isVar = false,
            name = "tmp0_iterator",
            irType = iteratorSymbol.defaultType,
            origin = IrDeclarationOrigin.FOR_LOOP_ITERATOR
        )
        return irBlock(
            type = unitType,
            origin = IrStatementOrigin.FOR_LOOP,
            statements = listOf(
                iteratorVar,
                IrWhileLoopImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    unitType,
                    IrStatementOrigin.FOR_LOOP_INNER_WHILE
                ).apply {
                    val loopVar = irTemporary(
                        containingDeclaration = scope,
                        value = irCall(
                            symbol = nextSymbol,
                            origin = IrStatementOrigin.FOR_LOOP_NEXT,
                            dispatchReceiver = irGet(iteratorVar)
                        ),
                        origin = IrDeclarationOrigin.FOR_LOOP_VARIABLE,
                        isVar = false,
                        name = "value",
                        irType = elementType
                    )
                    condition = irCall(
                        symbol = hasNextSymbol,
                        origin = IrStatementOrigin.FOR_LOOP_HAS_NEXT,
                        dispatchReceiver = irGet(iteratorVar)
                    )
                    body = irBlock(
                        type = unitType,
                        origin = IrStatementOrigin.FOR_LOOP_INNER_WHILE,
                        statements = listOf(
                            loopVar,
                            loopBody(loopVar)
                        )
                    )
                }
            )
        )
    }

    protected fun irTemporary(
        containingDeclaration: DeclarationDescriptor,
        value: IrExpression,
        name: String,
        irType: IrType = value.type,
        isVar: Boolean = false,
        origin: IrDeclarationOrigin = IrDeclarationOrigin.IR_TEMPORARY_VARIABLE
    ): IrVariableImpl {
        val tempVarDescriptor = IrTemporaryVariableDescriptorImpl(
            containingDeclaration,
            Name.identifier(name),
            irType.toKotlinType(),
            isVar
        )
        return IrVariableImpl(
            value.startOffset,
            value.endOffset,
            origin,
            tempVarDescriptor,
            irType,
            value
        )
    }

    protected fun irGet(type: IrType, symbol: IrValueSymbol): IrExpression {
        return IrGetValueImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            type,
            symbol
        )
    }

    protected fun irGet(variable: IrValueDeclaration): IrExpression {
        return irGet(variable.type, variable.symbol)
    }

    protected fun irIf(condition: IrExpression, body: IrExpression): IrExpression {
        return IrIfThenElseImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            context.irBuiltIns.unitType,
            origin = IrStatementOrigin.IF
        ).also {
            it.branches.add(
                IrBranchImpl(condition, body)
            )
        }
    }

    protected fun irIfThenElse(
        type: IrType = context.irBuiltIns.unitType,
        condition: IrExpression,
        thenPart: IrExpression,
        elsePart: IrExpression
    ) =
        IrIfThenElseImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, type, IrStatementOrigin.IF).apply {
            branches.add(IrBranchImpl(startOffset, endOffset, condition, thenPart))
            branches.add(irElseBranch(elsePart))
        }

    protected fun irWhen(
        type: IrType = context.irBuiltIns.unitType,
        origin: IrStatementOrigin? = null,
        branches: List<IrBranch>
    ) = IrWhenImpl(
        UNDEFINED_OFFSET,
        UNDEFINED_OFFSET,
        type,
        origin,
        branches
    )
    protected fun irBranch(
        condition: IrExpression,
        result: IrExpression
    ): IrBranch {
        return IrBranchImpl(condition, result)
    }

    protected fun irElseBranch(expression: IrExpression) =
        IrElseBranchImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, irConst(true), expression)

    protected fun irBlock(
        type: IrType = context.irBuiltIns.unitType,
        origin: IrStatementOrigin? = null,
        statements: List<IrStatement>
    ): IrExpression {
        return IrBlockImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            type,
            origin,
            statements
        )
    }

    protected fun irComposite(
        type: IrType = context.irBuiltIns.unitType,
        origin: IrStatementOrigin? = null,
        statements: List<IrStatement>
    ): IrExpression {
        return IrCompositeImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            type,
            origin,
            statements
        )
    }

    protected fun irLambda(function: IrFunction, type: IrType): IrExpression {
        return irBlock(
            type,
            origin = IrStatementOrigin.LAMBDA,
            statements = listOf(
                function,
                IrFunctionReferenceImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    type,
                    function.symbol,
                    function.typeParameters.size,
                    IrStatementOrigin.LAMBDA
                )
            )
        )
    }
}

fun IrFunction.composerParam(): IrValueParameter? {
    for (param in valueParameters.asReversed()) {
        if (param.isComposerParam()) return param
        if (!param.name.asString().startsWith('$')) return null
    }
    return null
}

fun IrValueParameter.isComposerParam(): Boolean =
    (descriptor as? ValueParameterDescriptor)?.isComposerParam() ?: false

fun ValueParameterDescriptor.isComposerParam(): Boolean =
    name == KtxNameConventions.COMPOSER_PARAMETER &&
            type.constructor.declarationDescriptor?.fqNameSafe == ComposeFqNames.Composer

object COMPOSE_STATEMENT_ORIGIN : IrStatementOriginImpl("COMPOSE_STATEMENT_ORIGIN")
