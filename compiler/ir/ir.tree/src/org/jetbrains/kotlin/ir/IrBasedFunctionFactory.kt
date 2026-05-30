/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir

import org.jetbrains.kotlin.builtins.FunctionInterfacePackageFragment
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.builtins.functions.FunctionClassDescriptor
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.PackageFragmentDescriptorImpl
import org.jetbrains.kotlin.ir.builders.declarations.IrValueParameterBuilder
import org.jetbrains.kotlin.ir.builders.declarations.buildReceiverParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildValueParameter
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin.Companion.INSTANCE_RECEIVER
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.overrides.FakeOverrideBuilderStrategy
import org.jetbrains.kotlin.ir.overrides.IrFakeOverrideBuilder
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrClassSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrFileSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrTypeParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeSystemContext
import org.jetbrains.kotlin.ir.types.defaultTypeWithoutArguments
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeBuilder
import org.jetbrains.kotlin.ir.types.impl.buildSimpleType
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.types.typeWithParameters
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.NaiveSourceBasedFileEntryImpl
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.addFile
import org.jetbrains.kotlin.ir.util.createThisReceiverParameter
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.Variance
import java.io.File

@OptIn(ObsoleteDescriptorBasedAPI::class)
abstract class IrAbstractFunctionFactory {
    open fun functionClassDescriptor(arity: Int): FunctionClassDescriptor? = null
    open fun kFunctionClassDescriptor(arity: Int): FunctionClassDescriptor? = null
    open fun suspendFunctionClassDescriptor(arity: Int): FunctionClassDescriptor? = null
    open fun kSuspendFunctionClassDescriptor(arity: Int): FunctionClassDescriptor? = null

    open fun functionClassSignature(arity: Int): IdSignature? = null
    open fun kFunctionClassSignature(arity: Int): IdSignature? = null
    open fun suspendFunctionClassSignature(arity: Int): IdSignature? = null
    open fun kSuspendFunctionClassSignature(arity: Int): IdSignature? = null

    abstract fun functionN(arity: Int, declarator: SymbolTable.((IrClassSymbol) -> IrClass) -> IrClass): IrClass
    abstract fun kFunctionN(arity: Int, declarator: SymbolTable.((IrClassSymbol) -> IrClass) -> IrClass): IrClass
    abstract fun suspendFunctionN(arity: Int, declarator: SymbolTable.((IrClassSymbol) -> IrClass) -> IrClass): IrClass
    abstract fun kSuspendFunctionN(arity: Int, declarator: SymbolTable.((IrClassSymbol) -> IrClass) -> IrClass): IrClass

    private fun SymbolTable.declare(
        descriptor: FunctionClassDescriptor?,
        signature: IdSignature?,
        callback: (IrClassSymbol) -> IrClass,
    ): IrClass = when {
        descriptor != null -> {
            descriptorExtension.declareClass(descriptor) { symbol ->
                callback(symbol)
            }
        }
        signature != null -> {
            declareClass(signature, { IrClassSymbolImpl(descriptor = null, signature) }) { symbol ->
                callback(symbol)
            }
        }
        else -> error("No descriptor or signature was provided")
    }

    fun functionN(n: Int) = functionN(n) { callback ->
        val descriptor = functionClassDescriptor(n)
        val signature = functionClassSignature(n)
        declare(descriptor, signature, callback)
    }

    fun kFunctionN(n: Int): IrClass = kFunctionN(n) { callback ->
        val descriptor = kFunctionClassDescriptor(n)
        val signature = kFunctionClassSignature(n)
        declare(descriptor, signature, callback)
    }

    fun suspendFunctionN(n: Int): IrClass = suspendFunctionN(n) { callback ->
        val descriptor = suspendFunctionClassDescriptor(n)
        val signature = suspendFunctionClassSignature(n)
        declare(descriptor, signature, callback)
    }

    fun kSuspendFunctionN(n: Int): IrClass = kSuspendFunctionN(n) { callback ->
        val descriptor = kSuspendFunctionClassDescriptor(n)
        val signature = kSuspendFunctionClassSignature(n)
        declare(descriptor, signature, callback)
    }

    companion object {
        val classOrigin = IrDeclarationOriginImpl("FUNCTION_INTERFACE_CLASS")
        val memberOrigin = IrDeclarationOrigin.FUNCTION_INTERFACE_MEMBER
        internal const val offset = SYNTHETIC_OFFSET

        internal fun functionClassName(isK: Boolean, isSuspend: Boolean, arity: Int): String =
            "${if (isK) "K" else ""}${if (isSuspend) "Suspend" else ""}Function$arity"
    }
}

private var IrFile.isSyntheticForFunctionInterfaceFile: Boolean? by irAttribute(copyByDefault = true)

class IrBasedFunctionFactory(
    private val stdlibModule: IrModuleFragment,
    private val functionClass: IrClassSymbol,
    private val kFunctionClass: IrClassSymbol,
    anyClass: IrClassSymbol,
    private val symbolTable: SymbolTable,
    private val signatureComputer: (IrDeclaration) -> IdSignature,
    private val onCreateNewClass: (IrClass, IdSignature) -> Unit,
) : IrAbstractFunctionFactory() {
    companion object {
        private const val FUNCTION_TYPE_INTERFACE_DIR = "function-type-interface"
        private const val FUNCTION_TYPE_INTERFACE_FILE = "[K][Suspend]Functions"

        @OptIn(ObsoleteDescriptorBasedAPI::class)
        val IrPackageFragment.isFunctionInterfaceFile: Boolean
            get() {
                return when {
                    symbol.hasDescriptor && packageFragmentDescriptor is FunctionInterfacePackageFragment -> true
                    this is IrFile && this.isSyntheticForFunctionInterfaceFile == true -> true
                    else -> false
                }
            }
    }

    public var typeSystem: IrTypeSystemContext? = null

    private val anyNType: IrType = anyClass.defaultTypeWithoutArguments.makeNullable()

    private val kotlinPackageFragment: IrPackageFragment by lazy { makePackageAccessor(StandardNames.BUILT_INS_PACKAGE_FQ_NAME) }
    private val kotlinCoroutinesPackageFragment: IrPackageFragment by lazy { makePackageAccessor(StandardNames.COROUTINES_PACKAGE_FQ_NAME) }
    private val kotlinReflectPackageFragment: IrPackageFragment by lazy { makePackageAccessor(StandardNames.KOTLIN_REFLECT_FQ_NAME) }

    fun makePackageAccessor(packageFqName: FqName): IrFile {
        // Standard behavior for JS and Wasm backends
        fun findFileInStdlib(fileName: String): IrFile? = stdlibModule.files.singleOrNull {
            // Do not check by name "$FUNCTION_TYPE_INTERFACE_DIR/$fileWithRequiredPackage" because the path separator depends on OS
            it.fileEntry.name.endsWith(fileName) && it.fileEntry.name.contains(FUNCTION_TYPE_INTERFACE_DIR)
        }

        // Standard behavior for Native backend
        fun createNewFile(): IrFile {
            val fileEntry = NaiveSourceBasedFileEntryImpl(FUNCTION_TYPE_INTERFACE_FILE)
            // TODO remove descriptor hack after #KT-81659
            val descriptor = object : PackageFragmentDescriptorImpl(stdlibModule.descriptor, packageFqName) {
                override fun getMemberScope(): MemberScope = error("Should not be called")
            }
            return IrFileImpl(fileEntry, IrFileSymbolImpl(descriptor), packageFqName, stdlibModule)
                .also { stdlibModule.addFile(it) }
        }

        val fileWithRequiredPackage = "${packageFqName.asString().replace('.', '-')}-package.kt"
        return (findFileInStdlib(fileWithRequiredPackage) ?: createNewFile())
            .also { it.isSyntheticForFunctionInterfaceFile = true }
    }

    private val functionNMap = mutableMapOf<Int, IrClass>()
    private val kFunctionNMap = mutableMapOf<Int, IrClass>()
    private val suspendFunctionNMap = mutableMapOf<Int, IrClass>()
    private val kSuspendFunctionNMap = mutableMapOf<Int, IrClass>()

    private val irFactory: IrFactory get() = symbolTable.irFactory

    override fun functionClassSignature(arity: Int): IdSignature {
        return StandardClassIds.FunctionN(arity).toSignature()
    }

    override fun suspendFunctionClassSignature(arity: Int): IdSignature {
        return StandardClassIds.SuspendFunctionN(arity).toSignature()
    }

    override fun kFunctionClassSignature(arity: Int): IdSignature {
        return StandardClassIds.KFunctionN(arity).toSignature()
    }

    override fun kSuspendFunctionClassSignature(arity: Int): IdSignature {
        return StandardClassIds.KSuspendFunctionN(arity).toSignature()
    }

    private fun ClassId.toSignature(): IdSignature {
        return IdSignature.CommonSignature(
            packageFqName = packageFqName.asString(),
            declarationFqName = shortClassName.asString(),
            id = null,
            mask = 0L,
            description = null
        )
    }

    override fun functionN(arity: Int, declarator: SymbolTable.((IrClassSymbol) -> IrClass) -> IrClass): IrClass {
        return functionNMap.getOrPut(arity) {
            symbolTable.declarator { symbol ->
                createFunctionClass(symbol, isK = false, isSuspend = false, arity, [functionClass], kotlinPackageFragment)
            }
        }
    }

    override fun suspendFunctionN(arity: Int, declarator: SymbolTable.((IrClassSymbol) -> IrClass) -> IrClass): IrClass {
        return suspendFunctionNMap.getOrPut(arity) {
            symbolTable.declarator { symbol ->
                createFunctionClass(symbol, isK = false, isSuspend = true, arity, [functionClass], kotlinCoroutinesPackageFragment)
            }
        }
    }

    override fun kFunctionN(arity: Int, declarator: SymbolTable.((IrClassSymbol) -> IrClass) -> IrClass): IrClass {
        val functionNClass = functionN(arity).symbol
        return kFunctionNMap.getOrPut(arity) {
            symbolTable.declarator { symbol ->
                createFunctionClass(symbol, isK = true, isSuspend = false, arity,
                                    [kFunctionClass, functionNClass], kotlinReflectPackageFragment)
            }
        }
    }

    override fun kSuspendFunctionN(arity: Int, declarator: SymbolTable.((IrClassSymbol) -> IrClass) -> IrClass): IrClass {
        val functionNClass = suspendFunctionN(arity).symbol
        return kSuspendFunctionNMap.getOrPut(arity) {
            symbolTable.declarator { symbol ->
                createFunctionClass(symbol, isK = true, isSuspend = true, arity,
                                    [kFunctionClass, functionNClass], kotlinReflectPackageFragment)
            }
        }
    }

    private fun IrTypeParametersContainer.createTypeParameters(n: Int): IrTypeParameter {
        val typeParametersArray = ArrayList<IrTypeParameter>(n + 1)

        fun createTypeParameter(index: Int, pName: Name, variance: Variance): IrTypeParameter {
            fun parameter(symbol: IrTypeParameterSymbol): IrTypeParameter = irFactory.createTypeParameter(
                startOffset = offset,
                endOffset = offset,
                origin = classOrigin,
                name = pName,
                symbol = symbol,
                variance = variance,
                index = index,
                isReified = false
            )

            val declarationWithoutSignature = parameter(IrTypeParameterSymbolImpl())
            declarationWithoutSignature.superTypes += anyNType
            declarationWithoutSignature.parent = this

            val signature = signatureComputer(declarationWithoutSignature)
            val declarationWithSignature = symbolTable.declareGlobalTypeParameter(
                signature,
                { IrTypeParameterSymbolImpl(descriptor = null, signature) }
            ) { parameter(it) }
            declarationWithSignature.superTypes += anyNType
            declarationWithSignature.parent = this

            typeParametersArray.add(declarationWithSignature)
            return declarationWithSignature
        }

        var index = 0
        for (i in 1 until (n + 1)) {
            val pName = Name.identifier("P$i")
            createTypeParameter(index++, pName, Variance.IN_VARIANCE)
        }
        val rDeclaration = createTypeParameter(index, Name.identifier("R"), Variance.OUT_VARIANCE)

        typeParameters = typeParametersArray

        return rDeclaration
    }

    private fun IrClass.createMembers(isK: Boolean, isSuspend: Boolean) {
        if (isK) return

        val returnType = with(IrSimpleTypeBuilder()) {
            classifier = typeParameters.last().symbol
            buildSimpleType()
        }

        val fDeclaration = irFactory.createFunctionWithLateBinding(
            startOffset = offset,
            endOffset = offset,
            origin = memberOrigin,
            name = Name.identifier("invoke"),
            visibility = DescriptorVisibilities.PUBLIC,
            isInline = false,
            isExpect = false,
            returnType = returnType,
            modality = Modality.ABSTRACT,
            isTailrec = false,
            isSuspend = isSuspend,
            isOperator = true,
            isInfix = false,
            isExternal = false,
            isFakeOverride = false
        )

        fDeclaration.parameters += fDeclaration.buildReceiverParameter {
            startOffset = offset
            endOffset = offset
            origin = INSTANCE_RECEIVER
            type = this@createMembers.symbol.typeWithParameters(this@createMembers.typeParameters)
        }

        for (i in 1 until typeParameters.size) {
            val vTypeParam = typeParameters[i - 1]
            val vDeclaration = IrValueParameterBuilder().run {
                startOffset = offset
                endOffset = offset
                origin = memberOrigin
                name = Name.identifier("p$i")
                type = vTypeParam.symbol.typeWith()
                factory.buildValueParameter(this, fDeclaration)
            }
            fDeclaration.parameters += vDeclaration
        }

        fDeclaration.parent = this
        declarations += fDeclaration

        val signature = signatureComputer(fDeclaration)
        val symbol = symbolTable.referenceSimpleFunction(signature)
        fDeclaration.acquireSymbol(symbol)
    }

    private fun createFunctionClass(
        symbol: IrClassSymbol,
        isK: Boolean,
        isSuspend: Boolean,
        n: Int,
        baseClasses: List<IrClassSymbol>,
        packageFragment: IrPackageFragment,
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

        klass.parent = packageFragment
        packageFragment.declarations += klass

        val r = klass.createTypeParameters(n)
        klass.createThisReceiverParameter()
        klass.thisReceiver!!.apply {
            startOffset = offset
            endOffset = offset
        }

        // Create `invoke` member before setting the super types as a hack for Native mangler.
        // Otherwise, the mangler will fail on call to `isObjCClassMethod` because super type `Function` is unbound.
        klass.createMembers(isK, isSuspend)

        klass.superTypes = baseClasses.map { baseClass ->
            val args = when (baseClass) {
                functionClass, kFunctionClass -> [r.symbol]
                else -> klass.typeParameters.map { it.symbol }
            }
            baseClass.typeWith(args.map { it.typeWith() })
        }

        klass.createFakeOverrides()
        onCreateNewClass(klass, symbol.signature!!)

        return klass
    }

    private fun IrClass.createFakeOverrides() {
        // During the linking process we do not build fake overrides. They will be built by fakeOverrideBuilder later on.
        // After the linking process we must build fake overrides (typeSystem will be set to the correct value) because fakeOverrideBuilder won't be called.
        if (typeSystem == null) return
        IrFakeOverrideBuilder(typeSystem!!, FunctionTypeFakeOverrideBuilderStrategy(), [])
            .buildFakeOverridesForClass(
                clazz = this,
                oldSignatures = false
            )
    }

    private inner class FunctionTypeFakeOverrideBuilderStrategy: FakeOverrideBuilderStrategy.BindToPrivateSymbols() {
        override fun postProcessGeneratedFakeOverride(fakeOverride: IrOverridableDeclaration<*>, clazz: IrClass) {}

        override fun shouldSeeInternals(thisModule: ModuleDescriptor, memberModule: ModuleDescriptor): Boolean = false

        override fun linkFunctionFakeOverride(function: IrFunctionWithLateBinding, manglerCompatibleMode: Boolean) {
            val signature = signatureComputer(function)
            val symbol = symbolTable.referenceSimpleFunction(signature)
            function.acquireSymbol(symbol)
        }

        override fun linkPropertyFakeOverride(property: IrPropertyWithLateBinding, manglerCompatibleMode: Boolean) {
            val signature = signatureComputer(property)
            val symbol = symbolTable.referenceProperty(signature)
            property.acquireSymbol(symbol)

            property.getter?.let { getter ->
                linkFunctionFakeOverride(
                    getter as? IrFunctionWithLateBinding ?: error("Unexpected fake override getter: $getter"),
                    manglerCompatibleMode
                )
            }
            property.setter?.let { setter ->
                linkFunctionFakeOverride(
                    setter as? IrFunctionWithLateBinding ?: error("Unexpected fake override setter: $setter"),
                    manglerCompatibleMode
                )
            }
        }
    }
}

