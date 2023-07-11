/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.descriptors

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.StandardNames.KOTLIN_REFLECT_FQ_NAME
import org.jetbrains.kotlin.builtins.functions.FunctionClassDescriptor
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.builders.declarations.UNDEFINED_PARAMETER_INDEX
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.SimpleTypeNullability
import org.jetbrains.kotlin.ir.types.impl.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.psi2ir.descriptors.IrBuiltInsOverDescriptors
import org.jetbrains.kotlin.resolve.descriptorUtil.isEffectivelyExternal
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.filterIsInstanceAnd
import org.jetbrains.kotlin.utils.memoryOptimizedMap

@OptIn(ObsoleteDescriptorBasedAPI::class)
abstract class IrAbstractDescriptorBasedFunctionFactory {

    abstract fun functionClassDescriptor(arity: Int): FunctionClassDescriptor
    abstract fun kFunctionClassDescriptor(arity: Int): FunctionClassDescriptor
    abstract fun suspendFunctionClassDescriptor(arity: Int): FunctionClassDescriptor
    abstract fun kSuspendFunctionClassDescriptor(arity: Int): FunctionClassDescriptor

    abstract fun functionN(arity: Int, declarator: SymbolTable.((IrClassSymbol) -> IrClass) -> IrClass): IrClass
    abstract fun kFunctionN(arity: Int, declarator: SymbolTable.((IrClassSymbol) -> IrClass) -> IrClass): IrClass
    abstract fun suspendFunctionN(arity: Int, declarator: SymbolTable.((IrClassSymbol) -> IrClass) -> IrClass): IrClass
    abstract fun kSuspendFunctionN(arity: Int, declarator: SymbolTable.((IrClassSymbol) -> IrClass) -> IrClass): IrClass

    fun functionN(n: Int) = functionN(n) { callback ->
        val descriptor = functionClassDescriptor(n)
        descriptorExtension.declareClass(descriptor) { symbol ->
            callback(symbol)
        }
    }

    fun kFunctionN(n: Int): IrClass {
        return kFunctionN(n) { callback ->
            val descriptor = kFunctionClassDescriptor(n)
            descriptorExtension.declareClass(descriptor) { symbol ->
                callback(symbol)
            }
        }
    }

    fun suspendFunctionN(n: Int): IrClass = suspendFunctionN(n) { callback ->
        val descriptor = suspendFunctionClassDescriptor(n)
        descriptorExtension.declareClass(descriptor) { symbol ->
            callback(symbol)
        }
    }

    fun kSuspendFunctionN(n: Int): IrClass = kSuspendFunctionN(n) { callback ->
        val descriptor = kSuspendFunctionClassDescriptor(n)
        descriptorExtension.declareClass(descriptor) { symbol ->
            callback(symbol)
        }
    }

    companion object {
        val classOrigin = object : IrDeclarationOriginImpl("FUNCTION_INTERFACE_CLASS") {}
        val memberOrigin = object : IrDeclarationOriginImpl("FUNCTION_INTERFACE_MEMBER") {}
        const val offset = SYNTHETIC_OFFSET

        internal fun functionClassName(isK: Boolean, isSuspend: Boolean, arity: Int): String =
            "${if (isK) "K" else ""}${if (isSuspend) "Suspend" else ""}Function$arity"
    }
}

@OptIn(ObsoleteDescriptorBasedAPI::class)
class IrDescriptorBasedFunctionFactory(
    private val irBuiltIns: IrBuiltInsOverDescriptors,
    private val symbolTable: SymbolTable,
    private val typeTranslator: TypeTranslator,
    getPackageFragment: ((PackageFragmentDescriptor) -> IrPackageFragment)? = null,
    // Needed for JS and Wasm backends to "preload" interfaces that can referenced during lowerings
    private val referenceFunctionsWhenKFunctionAreReferenced: Boolean = false,
) : IrAbstractDescriptorBasedFunctionFactory() {
    val getPackageFragment =
        getPackageFragment ?: symbolTable.descriptorExtension::declareExternalPackageFragmentIfNotExists

    // TODO: Lazieness

    private val functionNMap = mutableMapOf<Int, IrClass>()
    private val kFunctionNMap = mutableMapOf<Int, IrClass>()
    private val suspendFunctionNMap = mutableMapOf<Int, IrClass>()
    private val kSuspendFunctionNMap = mutableMapOf<Int, IrClass>()

    private val irFactory: IrFactory get() = symbolTable.irFactory

    val functionClass =
        symbolTable.descriptorExtension.referenceClass(irBuiltIns.builtIns.getBuiltInClassByFqName(FqName("kotlin.Function")))
    val kFunctionClass =
        symbolTable.descriptorExtension.referenceClass(irBuiltIns.builtIns.getBuiltInClassByFqName(FqName("kotlin.reflect.KFunction")))

    override fun functionClassDescriptor(arity: Int): FunctionClassDescriptor =
        irBuiltIns.builtIns.getFunction(arity) as FunctionClassDescriptor

    override fun suspendFunctionClassDescriptor(arity: Int): FunctionClassDescriptor =
        irBuiltIns.builtIns.getSuspendFunction(arity) as FunctionClassDescriptor

    override fun kFunctionClassDescriptor(arity: Int): FunctionClassDescriptor {
        val kFunctionFqn = reflectFunctionClassFqn(reflectionFunctionClassName(false, arity))
        return irBuiltIns.builtIns.getBuiltInClassByFqName(kFunctionFqn) as FunctionClassDescriptor
    }

    override fun kSuspendFunctionClassDescriptor(arity: Int): FunctionClassDescriptor {
        val kFunctionFqn = reflectFunctionClassFqn(reflectionFunctionClassName(true, arity))
        return irBuiltIns.builtIns.getBuiltInClassByFqName(kFunctionFqn) as FunctionClassDescriptor
    }

    override fun functionN(arity: Int, declarator: SymbolTable.((IrClassSymbol) -> IrClass) -> IrClass): IrClass {
        return functionNMap.getOrPut(arity) {
            symbolTable.declarator { symbol ->
                val descriptor = symbol.descriptor
                val descriptorFactory = FunctionDescriptorFactory.RealDescriptorFactory(descriptor, symbolTable)
                createFunctionClass(symbol, false, false, arity, irBuiltIns.functionClass, kotlinPackageFragment, descriptorFactory)
            }
        }
    }

    override fun suspendFunctionN(arity: Int, declarator: SymbolTable.((IrClassSymbol) -> IrClass) -> IrClass): IrClass {
        return suspendFunctionNMap.getOrPut(arity) {
            symbolTable.declarator { symbol ->
                val descriptor = symbol.descriptor as FunctionClassDescriptor
                val descriptorFactory = FunctionDescriptorFactory.RealDescriptorFactory(descriptor, symbolTable)
                createFunctionClass(symbol, false, true, arity, irBuiltIns.functionClass, kotlinCoroutinesPackageFragment, descriptorFactory)
            }
        }
    }

    override fun kFunctionN(arity: Int, declarator: SymbolTable.((IrClassSymbol) -> IrClass) -> IrClass): IrClass {
        if (referenceFunctionsWhenKFunctionAreReferenced)
            functionN(arity)

        return kFunctionNMap.getOrPut(arity) {
            symbolTable.declarator { symbol ->
                val descriptor = symbol.descriptor as FunctionClassDescriptor
                val descriptorFactory = FunctionDescriptorFactory.RealDescriptorFactory(descriptor, symbolTable)
                createFunctionClass(symbol, true, false, arity, irBuiltIns.kFunctionClass, kotlinReflectPackageFragment, descriptorFactory)
            }
        }
    }

    override fun kSuspendFunctionN(arity: Int, declarator: SymbolTable.((IrClassSymbol) -> IrClass) -> IrClass): IrClass {
        if (referenceFunctionsWhenKFunctionAreReferenced)
            suspendFunctionN(arity)

        return kSuspendFunctionNMap.getOrPut(arity) {
            symbolTable.declarator { symbol ->
                val descriptor = symbol.descriptor as FunctionClassDescriptor
                val descriptorFactory = FunctionDescriptorFactory.RealDescriptorFactory(descriptor, symbolTable)
                createFunctionClass(symbol, true, true, arity, irBuiltIns.kFunctionClass, kotlinReflectPackageFragment, descriptorFactory)
            }
        }
    }

    private sealed class FunctionDescriptorFactory(protected val symbolTable: SymbolTable) {
        abstract fun memberDescriptor(name: String, factory: (IrSimpleFunctionSymbol) -> IrSimpleFunction): IrSimpleFunctionSymbol
        abstract fun FunctionDescriptor.valueParameterDescriptor(index: Int): ValueParameterDescriptor
        abstract fun typeParameterDescriptor(index: Int, factory: (IrTypeParameterSymbol) -> IrTypeParameter): IrTypeParameterSymbol
        abstract fun classReceiverParameterDescriptor(): ReceiverParameterDescriptor

        class RealDescriptorFactory(private val classDescriptor: ClassDescriptor, symbolTable: SymbolTable) :
            FunctionDescriptorFactory(symbolTable) {
            override fun memberDescriptor(name: String, factory: (IrSimpleFunctionSymbol) -> IrSimpleFunction): IrSimpleFunctionSymbol {
                val descriptor = classDescriptor.unsubstitutedMemberScope.run {
                    if (name[0] == '<') {
                        val propertyName = name.drop(5).dropLast(1)
                        val property = getContributedVariables(Name.identifier(propertyName), NoLookupLocation.FROM_BACKEND).single()
                        property.accessors.first { it.name.asString() == name }
                    } else {
                        getContributedFunctions(Name.identifier(name), NoLookupLocation.FROM_BACKEND).first()
                    }
                }
                return symbolTable.descriptorExtension.declareSimpleFunction(descriptor, factory).symbol
            }

            override fun FunctionDescriptor.valueParameterDescriptor(index: Int): ValueParameterDescriptor {
                assert(containingDeclaration === classDescriptor)
                return valueParameters[index]
            }

            override fun typeParameterDescriptor(index: Int, factory: (IrTypeParameterSymbol) -> IrTypeParameter): IrTypeParameterSymbol {
                val descriptor = classDescriptor.declaredTypeParameters[index]
                return symbolTable.descriptorExtension.declareGlobalTypeParameter(descriptor, factory).symbol
            }

            override fun classReceiverParameterDescriptor(): ReceiverParameterDescriptor {
                return classDescriptor.thisAsReceiverParameter
            }
        }
    }

    private fun IrTypeParametersContainer.createTypeParameters(n: Int, descriptorFactory: FunctionDescriptorFactory): IrTypeParameter {

        var index = 0

        val typeParametersArray = ArrayList<IrTypeParameter>(n + 1)

        for (i in 1 until (n + 1)) {
            val pName = Name.identifier("P$i")

            val pSymbol = descriptorFactory.typeParameterDescriptor(index) {
                irFactory.createTypeParameter(
                    startOffset = offset,
                    endOffset = offset,
                    origin = classOrigin,
                    name = pName,
                    symbol = it,
                    variance = Variance.IN_VARIANCE,
                    index = index++,
                    isReified = false
                )
            }
            val pDeclaration = pSymbol.owner

            pDeclaration.superTypes += irBuiltIns.anyNType
            pDeclaration.parent = this
            typeParametersArray.add(pDeclaration)
        }

        val rSymbol = descriptorFactory.typeParameterDescriptor(index) {
            irFactory.createTypeParameter(
                startOffset = offset,
                endOffset = offset,
                origin = classOrigin,
                name = Name.identifier("R"),
                symbol = it,
                variance = Variance.OUT_VARIANCE,
                index = index,
                isReified = false
            )
        }
        val rDeclaration = rSymbol.owner

        rDeclaration.superTypes += irBuiltIns.anyNType
        rDeclaration.parent = this
        typeParametersArray.add(rDeclaration)

        typeParameters = typeParametersArray

        return rDeclaration
    }

    private val kotlinPackageFragment: IrPackageFragment by lazy {
        irBuiltIns.builtIns.getFunction(0).let {
            getPackageFragment(it.containingDeclaration as PackageFragmentDescriptor)
        }
    }
    private val kotlinCoroutinesPackageFragment: IrPackageFragment by lazy {
        irBuiltIns.builtIns.getSuspendFunction(0).let {
            getPackageFragment(it.containingDeclaration as PackageFragmentDescriptor)
        }
    }

    private val kotlinReflectPackageFragment: IrPackageFragment by lazy {
        irBuiltIns.kPropertyClass.descriptor.let {
            getPackageFragment(it.containingDeclaration as PackageFragmentDescriptor)
        }
    }

    private fun createThisReceiver(descriptorFactory: FunctionDescriptorFactory): IrValueParameter {
        val descriptor = descriptorFactory.classReceiverParameterDescriptor()
        return irFactory.createValueParameter(
            startOffset = offset,
            endOffset = offset,
            origin = classOrigin,
            name = SpecialNames.THIS,
            type = typeTranslator.translateType(descriptor.type),
            isAssignable = false,
            symbol = IrValueParameterSymbolImpl(descriptor),
            index = UNDEFINED_PARAMETER_INDEX,
            varargElementType = null,
            isCrossinline = false,
            isNoinline = false,
            isHidden = false,
        )
    }

    private fun IrClass.createMembers(isK: Boolean, isSuspend: Boolean, descriptorFactory: FunctionDescriptorFactory) {
        if (!isK) {
            val invokeSymbol = descriptorFactory.memberDescriptor("invoke") {
                val returnType = with(IrSimpleTypeBuilder()) {
                    classifier = typeParameters.last().symbol
                    buildSimpleType()
                }

                irFactory.createSimpleFunction(
                    startOffset = offset,
                    endOffset = offset,
                    origin = memberOrigin,
                    name = Name.identifier("invoke"),
                    visibility = DescriptorVisibilities.PUBLIC,
                    isInline = false,
                    isExpect = false,
                    returnType = returnType,
                    modality = Modality.ABSTRACT,
                    symbol = it,
                    isTailrec = false,
                    isSuspend = isSuspend,
                    isOperator = true,
                    isInfix = false,
                    isExternal = false,
                    isFakeOverride = false
                )
            }

            val fDeclaration = invokeSymbol.owner

            fDeclaration.dispatchReceiverParameter = createThisReceiver(descriptorFactory).also { it.parent = fDeclaration }

            val typeBuilder = IrSimpleTypeBuilder()
            for (i in 1 until typeParameters.size) {
                val vTypeParam = typeParameters[i - 1]
                val vDescriptor = with(descriptorFactory) { invokeSymbol.descriptor.valueParameterDescriptor(i - 1) }
                val vSymbol = IrValueParameterSymbolImpl(vDescriptor)
                val vType = with(typeBuilder) {
                    classifier = vTypeParam.symbol
                    buildSimpleType()
                }
                val vDeclaration = irFactory.createValueParameter(
                    startOffset = offset,
                    endOffset = offset,
                    origin = memberOrigin,
                    name = Name.identifier("p$i"),
                    type = vType,
                    isAssignable = false,
                    symbol = vSymbol,
                    index = i - 1,
                    varargElementType = null,
                    isCrossinline = false,
                    isNoinline = false,
                    isHidden = false,
                )
                vDeclaration.parent = fDeclaration
                fDeclaration.valueParameters += vDeclaration
            }

            fDeclaration.parent = this
            declarations += fDeclaration
        }

        // TODO: eventualy delegate it to fakeOverrideBuilder
        addFakeOverrides()
    }

    private fun toIrType(wrapped: KotlinType): IrType {
        val kotlinType = wrapped.unwrap()
        return with(IrSimpleTypeBuilder()) {
            classifier =
                symbolTable.referenceClassifier(kotlinType.constructor.declarationDescriptor ?: error("No classifier for type $kotlinType"))
            nullability = SimpleTypeNullability.fromHasQuestionMark(kotlinType.isMarkedNullable)
            arguments = kotlinType.arguments.memoryOptimizedMap {
                if (it.isStarProjection) IrStarProjectionImpl
                else makeTypeProjection(toIrType(it.type), it.projectionKind)
            }
            buildSimpleType()
        }
    }

    private fun IrFunction.createValueParameter(descriptor: ParameterDescriptor): IrValueParameter = with(descriptor) {
        irFactory.createValueParameter(
            startOffset = offset,
            endOffset = offset,
            origin = memberOrigin,
            name = name,
            type = toIrType(type),
            isAssignable = false,
            symbol = IrValueParameterSymbolImpl(this),
            index = indexOrMinusOne,
            varargElementType = (this as? ValueParameterDescriptor)?.varargElementType?.let(::toIrType),
            isCrossinline = isCrossinline,
            isNoinline = isNoinline,
            isHidden = false
        ).also {
            it.parent = this@createValueParameter
        }
    }

    private fun IrClass.addFakeOverrides() {

        val fakeOverrideDescriptors =
            descriptor.unsubstitutedMemberScope.getContributedDescriptors(DescriptorKindFilter.CALLABLES)
                .filterIsInstanceAnd<CallableMemberDescriptor> {
                    it.kind === CallableMemberDescriptor.Kind.FAKE_OVERRIDE && it.dispatchReceiverParameter != null &&
                            !DescriptorVisibilities.isPrivate(it.visibility) && it.visibility != DescriptorVisibilities.INVISIBLE_FAKE
                }

        fun createFakeOverrideFunction(descriptor: FunctionDescriptor, property: IrPropertySymbol?): IrSimpleFunction {
            val returnType = descriptor.returnType?.let { toIrType(it) } ?: error("No return type for $descriptor")
            val newFunction = symbolTable.descriptorExtension.declareSimpleFunction(descriptor) {
                descriptor.run {
                    irFactory.createSimpleFunction(
                        startOffset = offset,
                        endOffset = offset,
                        origin = memberOrigin,
                        name = name,
                        visibility = visibility,
                        isInline = isInline,
                        isExpect = isExpect,
                        returnType = returnType,
                        modality = modality,
                        symbol = it,
                        isTailrec = isTailrec,
                        isSuspend = isSuspend,
                        isOperator = isOperator,
                        isInfix = isInfix,
                        isExternal = isEffectivelyExternal(),
                        isFakeOverride = true,
                    )
                }
            }

            newFunction.parent = this
            newFunction.overriddenSymbols =
                descriptor.overriddenDescriptors.memoryOptimizedMap { symbolTable.descriptorExtension.referenceSimpleFunction(it.original) }
            newFunction.dispatchReceiverParameter = descriptor.dispatchReceiverParameter?.let { newFunction.createValueParameter(it) }
            newFunction.extensionReceiverParameter = descriptor.extensionReceiverParameter?.let { newFunction.createValueParameter(it) }
            newFunction.contextReceiverParametersCount = descriptor.contextReceiverParameters.size
            newFunction.valueParameters = descriptor.valueParameters.memoryOptimizedMap { newFunction.createValueParameter(it) }
            newFunction.correspondingPropertySymbol = property
            newFunction.annotations = descriptor.annotations.mapNotNull(
                typeTranslator.constantValueGenerator::generateAnnotationConstructorCall
            )

            return newFunction
        }

        fun createFakeOverrideProperty(descriptor: PropertyDescriptor): IrProperty {
            return symbolTable.descriptorExtension.declareProperty(descriptor) {
                irFactory.createProperty(
                    startOffset = offset,
                    endOffset = offset,
                    origin = memberOrigin,
                    name = descriptor.name,
                    visibility = descriptor.visibility,
                    modality = descriptor.modality,
                    symbol = it,
                    isVar = descriptor.isVar,
                    isConst = descriptor.isConst,
                    isLateinit = descriptor.isLateInit,
                    isDelegated = descriptor.isDelegated,
                    isExternal = descriptor.isEffectivelyExternal(),
                    isExpect = descriptor.isExpect,
                ).apply {
                    parent = this@addFakeOverrides
                    getter = descriptor.getter?.let { g -> createFakeOverrideFunction(g, symbol) }
                    setter = descriptor.setter?.let { s -> createFakeOverrideFunction(s, symbol) }
                    annotations = descriptor.annotations.mapNotNull(
                        typeTranslator.constantValueGenerator::generateAnnotationConstructorCall
                    )
                }
            }
        }


        fun createFakeOverride(descriptor: CallableMemberDescriptor): IrDeclaration {
            return when (descriptor) {
                is FunctionDescriptor -> createFakeOverrideFunction(descriptor, null)
                is PropertyDescriptor -> createFakeOverrideProperty(descriptor)
                else -> error("Unexpected member $descriptor")
            }
        }

        declarations += fakeOverrideDescriptors.map { createFakeOverride(it) }
    }

    private fun createFunctionClass(
        symbol: IrClassSymbol,
        isK: Boolean,
        isSuspend: Boolean,
        n: Int,
        baseClass: IrClassSymbol,
        packageFragment: IrPackageFragment,
        descriptorFactory: FunctionDescriptorFactory
    ): IrClass {
        val name = functionClassName(isK, isSuspend, n)
        if (symbol.isBound) return symbol.owner
        val klass = irFactory.createClass(
            startOffset = offset,
            endOffset = offset,
            origin = classOrigin,
            name = Name.identifier(name),
            visibility = DescriptorVisibilities.PUBLIC,
            symbol = symbol,
            kind = ClassKind.INTERFACE,
            modality = Modality.ABSTRACT,
        )

        val r = klass.createTypeParameters(n, descriptorFactory)

        klass.thisReceiver = createThisReceiver(descriptorFactory).also { it.parent = klass }

        klass.superTypes = listOf(with(IrSimpleTypeBuilder()) {
            classifier = baseClass
            arguments = listOf(
                with(IrSimpleTypeBuilder()) {
                    classifier = r.symbol
                    buildTypeProjection()
                },
            )
            buildSimpleType()
        })

        klass.parent = packageFragment
        packageFragment.declarations += klass

        klass.createMembers(isK, isSuspend, descriptorFactory)

        return klass
    }
}

private fun reflectFunctionClassFqn(shortName: Name): FqName = KOTLIN_REFLECT_FQ_NAME.child(shortName)
private fun reflectionFunctionClassName(isSuspend: Boolean, arity: Int): Name =
    Name.identifier("K${if (isSuspend) "Suspend" else ""}Function$arity")

fun KotlinBuiltIns.functionClassDescriptor(arity: Int): FunctionClassDescriptor =
    getFunction(arity) as FunctionClassDescriptor

fun KotlinBuiltIns.suspendFunctionClassDescriptor(arity: Int): FunctionClassDescriptor =
    getSuspendFunction(arity) as FunctionClassDescriptor

fun KotlinBuiltIns.kFunctionClassDescriptor(arity: Int): FunctionClassDescriptor {
    val kFunctionFqn = reflectFunctionClassFqn(reflectionFunctionClassName(false, arity))
    return getBuiltInClassByFqName(kFunctionFqn) as FunctionClassDescriptor
}

fun KotlinBuiltIns.kSuspendFunctionClassDescriptor(arity: Int): FunctionClassDescriptor {
    val kFunctionFqn =
        reflectFunctionClassFqn(reflectionFunctionClassName(true, arity))
    return getBuiltInClassByFqName(kFunctionFqn) as FunctionClassDescriptor
}
