/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.descriptors

import org.jetbrains.kotlin.builtins.KOTLIN_REFLECT_FQ_NAME
import org.jetbrains.kotlin.builtins.functions.FunctionClassDescriptor
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.DescriptorBasedIr
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.impl.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.isEffectivelyExternal
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.Variance

@OptIn(DescriptorBasedIr::class)
abstract class IrAbstractFunctionFactory {

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
        declareClass(descriptor) { symbol ->
            callback(symbol)
        }
    }

    fun kFunctionN(n: Int): IrClass {
        return kFunctionN(n) { callback ->
            val descriptor = kFunctionClassDescriptor(n)
            declareClass(descriptor) { symbol ->
                callback(symbol)
            }
        }
    }

    fun suspendFunctionN(n: Int): IrClass = suspendFunctionN(n) { callback ->
        val descriptor = suspendFunctionClassDescriptor(n)
        declareClass(descriptor) { symbol ->
            callback(symbol)
        }
    }

    fun kSuspendFunctionN(n: Int): IrClass = kSuspendFunctionN(n) { callback ->
        val descriptor = kSuspendFunctionClassDescriptor(n)
        declareClass(descriptor) { symbol ->
            callback(symbol)
        }
    }

    companion object {
        val classOrigin = object : IrDeclarationOriginImpl("FUNCTION_INTERFACE_CLASS") {}
        val memberOrigin = object : IrDeclarationOriginImpl("FUNCTION_INTERFACE_MEMBER") {}
        const val offset = SYNTHETIC_OFFSET
    }
}

@OptIn(DescriptorBasedIr::class)
class IrFunctionFactory(private val irBuiltIns: IrBuiltIns, private val symbolTable: SymbolTable) : IrAbstractFunctionFactory() {

    // TODO: Lazieness

    private val functionNMap = mutableMapOf<Int, IrClass>()
    private val kFunctionNMap = mutableMapOf<Int, IrClass>()
    private val suspendFunctionNMap = mutableMapOf<Int, IrClass>()
    private val kSuspendFunctionNMap = mutableMapOf<Int, IrClass>()

    override fun functionClassDescriptor(arity: Int): FunctionClassDescriptor =
        irBuiltIns.builtIns.getFunction(arity) as FunctionClassDescriptor

    override fun functionN(arity: Int, declarator: SymbolTable.((IrClassSymbol) -> IrClass) -> IrClass): IrClass {
        return functionNMap.getOrPut(arity) {
            symbolTable.declarator { symbol ->
                val descriptor = symbol.descriptor as FunctionClassDescriptor
                val descriptorFactory = FunctionDescriptorFactory.RealDescriptorFactory(descriptor, symbolTable)
                createFunctionClass(symbol, false, false, arity, irBuiltIns.functionClass, kotlinPackageFragment, descriptorFactory)
            }
        }
    }

    override fun suspendFunctionClassDescriptor(arity: Int): FunctionClassDescriptor =
        irBuiltIns.builtIns.getSuspendFunction(arity) as FunctionClassDescriptor

    override fun suspendFunctionN(arity: Int, declarator: SymbolTable.((IrClassSymbol) -> IrClass) -> IrClass): IrClass {
        return suspendFunctionNMap.getOrPut(arity) {
            symbolTable.declarator { symbol ->
                val descriptor = symbol.descriptor as FunctionClassDescriptor
                val descriptorFactory = FunctionDescriptorFactory.RealDescriptorFactory(descriptor, symbolTable)
                createFunctionClass(symbol, false, true, arity, irBuiltIns.functionClass, kotlinCoroutinesPackageFragment, descriptorFactory)
            }
        }
    }

    override fun kFunctionClassDescriptor(arity: Int): FunctionClassDescriptor {
        val kFunctionFqn = reflectFunctionClassFqn(reflectionFunctionClassName(false, arity))
        return irBuiltIns.builtIns.getBuiltInClassByFqName(kFunctionFqn) as FunctionClassDescriptor
    }

    override fun kFunctionN(arity: Int, declarator: SymbolTable.((IrClassSymbol) -> IrClass) -> IrClass): IrClass {
        return kFunctionNMap.getOrPut(arity) {
            symbolTable.declarator { symbol ->
                val descriptor = symbol.descriptor as FunctionClassDescriptor
                val descriptorFactory = FunctionDescriptorFactory.RealDescriptorFactory(descriptor, symbolTable)
                createFunctionClass(symbol, true, false, arity, irBuiltIns.kFunctionClass, kotlinReflectPackageFragment, descriptorFactory)
            }
        }
    }

    override fun kSuspendFunctionClassDescriptor(arity: Int): FunctionClassDescriptor {
        val kFunctionFqn = reflectFunctionClassFqn(reflectionFunctionClassName(true, arity))
        return irBuiltIns.builtIns.getBuiltInClassByFqName(kFunctionFqn) as FunctionClassDescriptor
    }

    override fun kSuspendFunctionN(arity: Int, declarator: SymbolTable.((IrClassSymbol) -> IrClass) -> IrClass): IrClass {
        return kSuspendFunctionNMap.getOrPut(arity) {
            symbolTable.declarator { symbol ->
                val descriptor = symbol.descriptor as FunctionClassDescriptor
                val descriptorFactory = FunctionDescriptorFactory.RealDescriptorFactory(descriptor, symbolTable)
                createFunctionClass(symbol, true, true, arity, irBuiltIns.kFunctionClass, kotlinReflectPackageFragment, descriptorFactory)
            }
        }
    }

    companion object {
        private fun reflectFunctionClassFqn(shortName: Name): FqName = KOTLIN_REFLECT_FQ_NAME.child(shortName)
        private fun reflectionFunctionClassName(isSuspend: Boolean, arity: Int): Name =
            Name.identifier("K${if (isSuspend) "Suspend" else ""}Function$arity")

        private fun functionClassName(isK: Boolean, isSuspend: Boolean, arity: Int): String =
            "${if (isK) "K" else ""}${if (isSuspend) "Suspend" else ""}Function$arity"
    }

    private sealed class FunctionDescriptorFactory(protected val symbolTable: SymbolTable) {
        abstract fun memberDescriptor(name: String, factory: (IrSimpleFunctionSymbol) -> IrSimpleFunction): IrSimpleFunctionSymbol
        abstract fun FunctionDescriptor.valueParameterDescriptor(index: Int): ValueParameterDescriptor
        abstract fun typeParameterDescriptor(index: Int, factory: (IrTypeParameterSymbol) -> IrTypeParameter): IrTypeParameterSymbol
        abstract fun classReceiverParameterDescriptor(): ReceiverParameterDescriptor
        abstract fun FunctionDescriptor.memberReceiverParameterDescriptor(): ReceiverParameterDescriptor

        class RealDescriptorFactory(private val classDescriptor: FunctionClassDescriptor, symbolTable: SymbolTable) :
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
                return symbolTable.declareSimpleFunction(descriptor, factory).symbol
            }

            override fun FunctionDescriptor.valueParameterDescriptor(index: Int): ValueParameterDescriptor {
                assert(containingDeclaration === classDescriptor)
                return valueParameters[index]
            }

            override fun typeParameterDescriptor(index: Int, factory: (IrTypeParameterSymbol) -> IrTypeParameter): IrTypeParameterSymbol {
                val descriptor = classDescriptor.declaredTypeParameters[index]
                return symbolTable.declareGlobalTypeParameter(offset, offset, classOrigin, descriptor, factory).symbol
            }

            override fun classReceiverParameterDescriptor(): ReceiverParameterDescriptor {
                return classDescriptor.thisAsReceiverParameter
            }

            override fun FunctionDescriptor.memberReceiverParameterDescriptor(): ReceiverParameterDescriptor {
                assert(containingDeclaration === classDescriptor)
                return dispatchReceiverParameter ?: error("Expected dispatch receiver at $this")
            }
        }
    }

    private fun IrTypeParametersContainer.createTypeParameters(n: Int, descriptorFactory: FunctionDescriptorFactory): IrTypeParameter {

        var index = 0

        val typeParametersArray = ArrayList<IrTypeParameter>(n + 1)

        for (i in 1 until (n + 1)) {
            val pName = Name.identifier("P$i")

            val pSymbol = descriptorFactory.typeParameterDescriptor(index) {
                IrTypeParameterImpl(offset, offset, classOrigin, it, pName, index++, false, Variance.IN_VARIANCE)
            }
            val pDeclaration = pSymbol.owner

            pDeclaration.superTypes += irBuiltIns.anyNType
            pDeclaration.parent = this
            typeParametersArray.add(pDeclaration)
        }

        val rSymbol = descriptorFactory.typeParameterDescriptor(index) {
            IrTypeParameterImpl(offset, offset, classOrigin, it, Name.identifier("R"), index, false, Variance.OUT_VARIANCE)
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
            symbolTable.declareExternalPackageFragment(it.containingDeclaration as PackageFragmentDescriptor)
        }
    }
    private val kotlinCoroutinesPackageFragment: IrPackageFragment by lazy {
        irBuiltIns.builtIns.getSuspendFunction(0).let {
            symbolTable.declareExternalPackageFragment(it.containingDeclaration as PackageFragmentDescriptor)
        }
    }

    private val kotlinReflectPackageFragment: IrPackageFragment by lazy {
        irBuiltIns.kPropertyClass.descriptor.let {
            symbolTable.declareExternalPackageFragment(it.containingDeclaration as PackageFragmentDescriptor)
        }
    }

    private fun IrClass.createThisReceiver(descriptorFactory: FunctionDescriptorFactory): IrValueParameter {
        val vDescriptor = descriptorFactory.classReceiverParameterDescriptor()
        val vSymbol = IrValueParameterSymbolImpl(vDescriptor)
        val type = with(IrSimpleTypeBuilder()) {
            classifier = symbol
            arguments = typeParameters.run {
                val builder = IrSimpleTypeBuilder()
                mapTo(ArrayList(size)) {
                    builder.classifier = it.symbol
                    buildTypeProjection()
                }
            }
            buildSimpleType()
        }
        val vDeclaration = IrValueParameterImpl(
            offset, offset, classOrigin, vSymbol, Name.special("<this>"), -1, type, null,
            isCrossinline = false,
            isNoinline = false
        )

        if (vDescriptor is WrappedReceiverParameterDescriptor) vDescriptor.bind(vDeclaration)

        return vDeclaration
    }

    private fun FunctionClassDescriptor.createFunctionClass(): IrClass {
        val s = symbolTable.referenceClass(this)
        if (s.isBound) return s.owner
        return symbolTable.declareClass(this) {
            val factory = FunctionDescriptorFactory.RealDescriptorFactory(this, symbolTable)
            when (functionKind) {
                FunctionClassDescriptor.Kind.Function ->
                    createFunctionClass(it, false, false, arity, irBuiltIns.functionClass, kotlinPackageFragment, factory)
                FunctionClassDescriptor.Kind.SuspendFunction ->
                    createFunctionClass(it, false, true, arity, irBuiltIns.functionClass, kotlinCoroutinesPackageFragment, factory)
                FunctionClassDescriptor.Kind.KFunction ->
                    createFunctionClass(it, true, false, arity, irBuiltIns.kFunctionClass, kotlinReflectPackageFragment, factory)
                FunctionClassDescriptor.Kind.KSuspendFunction ->
                    createFunctionClass(it, true, true, arity, irBuiltIns.kFunctionClass, kotlinReflectPackageFragment, factory)
            }
        }
    }

    private fun IrClass.createMembers(isK: Boolean, isSuspend: Boolean, arity: Int, name: String, descriptorFactory: FunctionDescriptorFactory) {
        if (!isK) {
            val invokeSymbol = descriptorFactory.memberDescriptor("invoke") {
                val returnType = with(IrSimpleTypeBuilder()) {
                    classifier = typeParameters.last().symbol
                    buildSimpleType()
                }

                IrFunctionImpl(
                    offset, offset, memberOrigin, it, Name.identifier("invoke"), Visibilities.PUBLIC, Modality.ABSTRACT,
                    returnType,
                    isInline = false,
                    isExternal = false,
                    isTailrec = false,
                    isSuspend = isSuspend,
                    isOperator = true,
                    isExpect = false,
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
                val vDeclaration = IrValueParameterImpl(
                    offset, offset, memberOrigin, vSymbol, Name.identifier("p$i"), i - 1, vType, null,
                    isCrossinline = false,
                    isNoinline = false
                )
                vDeclaration.parent = fDeclaration
                if (vDescriptor is WrappedValueParameterDescriptor) vDescriptor.bind(vDeclaration)
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
            hasQuestionMark = kotlinType.isMarkedNullable
            arguments = kotlinType.arguments.map {
                if (it.isStarProjection) IrStarProjectionImpl
                else makeTypeProjection(toIrType(it.type), it.projectionKind)
            }
            buildSimpleType()
        }
    }

    private fun IrFunction.createValueParameter(descriptor: ParameterDescriptor): IrValueParameter {
        val symbol = IrValueParameterSymbolImpl(descriptor)
        val varargType = if (descriptor is ValueParameterDescriptor) descriptor.varargElementType else null
        return IrValueParameterImpl(
            offset,
            offset,
            memberOrigin,
            descriptor,
            symbol = symbol,
            type = toIrType(descriptor.type),
            varargElementType = varargType?.let { toIrType(it) }
        ).also {
            it.parent = this
        }
    }

    private fun IrClass.addFakeOverrides() {

        val fakeOverrideDescriptors = descriptor.unsubstitutedMemberScope.getContributedDescriptors(DescriptorKindFilter.CALLABLES)
            .filterIsInstance<CallableMemberDescriptor>().filter { it.kind === CallableMemberDescriptor.Kind.FAKE_OVERRIDE }

        fun createFakeOverrideFunction(descriptor: FunctionDescriptor, property: IrPropertySymbol?): IrSimpleFunction {
            val returnType = descriptor.returnType?.let { toIrType(it) } ?: error("No return type for $descriptor")
            val newFunction = symbolTable.declareSimpleFunction(descriptor) {
                descriptor.run {
                    IrFunctionImpl(
                        offset, offset, memberOrigin, it, name, visibility, modality, returnType,
                        isInline, isExternal, isTailrec, isSuspend, isOperator, isExpect, true
                    )
                }
            }

            newFunction.parent = this
            newFunction.overriddenSymbols = descriptor.overriddenDescriptors.map { symbolTable.referenceSimpleFunction(it.original) }
            newFunction.dispatchReceiverParameter = descriptor.dispatchReceiverParameter?.let { newFunction.createValueParameter(it) }
            newFunction.extensionReceiverParameter = descriptor.extensionReceiverParameter?.let { newFunction.createValueParameter(it) }
            newFunction.valueParameters = descriptor.valueParameters.map { newFunction.createValueParameter(it) }
            newFunction.correspondingPropertySymbol = property

            return newFunction
        }

        fun createFakeOverrideProperty(descriptor: PropertyDescriptor): IrProperty {
            return symbolTable.declareProperty(offset, offset, memberOrigin, descriptor) {
                IrPropertyImpl(
                    offset, offset, memberOrigin, it,
                    name = descriptor.name,
                    visibility = descriptor.visibility,
                    modality = descriptor.modality,
                    isVar = descriptor.isVar,
                    isConst = descriptor.isConst,
                    isLateinit = descriptor.isLateInit,
                    isDelegated = descriptor.isDelegated,
                    isExternal = descriptor.isEffectivelyExternal(),
                    isExpect = descriptor.isExpect
                ).apply {
                    parent = this@addFakeOverrides
                    getter = descriptor.getter?.let { g -> createFakeOverrideFunction(g, symbol) }
                    setter = descriptor.setter?.let { s -> createFakeOverrideFunction(s, symbol) }
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
        val klass = IrClassImpl(
            offset, offset, classOrigin, symbol, Name.identifier(name), ClassKind.INTERFACE, Visibilities.PUBLIC, Modality.ABSTRACT
        )

        val r = klass.createTypeParameters(n, descriptorFactory)

        klass.thisReceiver = klass.createThisReceiver(descriptorFactory).also { it.parent = klass }

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

        klass.createMembers(isK, isSuspend, n, klass.name.identifier, descriptorFactory)

        return klass
    }
}