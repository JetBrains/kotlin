/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
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
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeBuilder
import org.jetbrains.kotlin.ir.types.impl.buildSimpleType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.Variance

private var IrFile.isSyntheticForFunctionInterfaceFile: Boolean? by irAttribute(copyByDefault = true)

class IrBasedFunctionFactory(
    private val stdlibModule: IrModuleFragment,
    private val functionClass: IrClassSymbol,
    private val kFunctionClass: IrClassSymbol,
    anyClass: IrClassSymbol,
    private val symbolTable: SymbolTable,
    private val signatureComputer: (IrDeclaration) -> IdSignature,
    private val onCreateNewClass: (IrClass, IdSignature) -> Unit,
) {
    companion object {
        val classOrigin = IrDeclarationOriginImpl("FUNCTION_INTERFACE_CLASS")
        val memberOrigin = IrDeclarationOrigin.FUNCTION_INTERFACE_MEMBER
        internal const val offset = SYNTHETIC_OFFSET
        private const val FUNCTION_TYPE_INTERFACE_DIR = "function-type-interface"
        private const val FUNCTION_TYPE_INTERFACE_FILE = "[K][Suspend]Functions"

        val IrPackageFragment.isFunctionInterfaceFile: Boolean
            get() = this is IrFile && this.isSyntheticForFunctionInterfaceFile == true

        internal fun functionClassName(isK: Boolean, isSuspend: Boolean, arity: Int): String =
            "${if (isK) "K" else ""}${if (isSuspend) "Suspend" else ""}Function$arity"
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

    private fun buildFunctionN(
        n: Int,
        cache: MutableMap<Int, IrClass>,
        signatureFactory: (Int) -> IdSignature,
        classFactory: (IrClassSymbol) -> IrClass
    ): IrClass {
        return cache.getOrPut(n) {
            val signature = signatureFactory(n)
            symbolTable.declareClass(
                signature,
                symbolFactory = { IrClassSymbolImpl(signature = signature) },
                classFactory = { symbol -> classFactory(symbol) }
            )
        }
    }

    fun functionN(n: Int): IrClass {
        return buildFunctionN(n, functionNMap, ::functionClassSignature) { symbol ->
            createFunctionClass(symbol, isK = false, isSuspend = false, n, listOf(functionClass), kotlinPackageFragment)
        }
    }

    fun kFunctionN(n: Int): IrClass {
        val functionNClass = functionN(n).symbol
        return buildFunctionN(n, kFunctionNMap, ::kFunctionClassSignature) { symbol ->
            createFunctionClass(symbol, isK = true, isSuspend = false, n, listOf(kFunctionClass, functionNClass), kotlinReflectPackageFragment)
        }
    }

    fun suspendFunctionN(n: Int): IrClass {
        return buildFunctionN(n, suspendFunctionNMap, ::suspendFunctionClassSignature) { symbol ->
            createFunctionClass(symbol, isK = false, isSuspend = true, n, listOf(functionClass), kotlinCoroutinesPackageFragment)
        }
    }

    fun kSuspendFunctionN(n: Int): IrClass {
        val functionNClass = suspendFunctionN(n).symbol
        return buildFunctionN(n, kSuspendFunctionNMap, ::kSuspendFunctionClassSignature) { symbol ->
            createFunctionClass(symbol, isK = true, isSuspend = true, n, listOf(kFunctionClass, functionNClass), kotlinReflectPackageFragment)
        }
    }

    private fun functionClassSignature(arity: Int): IdSignature {
        return StandardClassIds.FunctionN(arity).toSignature()
    }

    private fun suspendFunctionClassSignature(arity: Int): IdSignature {
        return StandardClassIds.SuspendFunctionN(arity).toSignature()
    }

    private fun kFunctionClassSignature(arity: Int): IdSignature {
        return StandardClassIds.KFunctionN(arity).toSignature()
    }

    private fun kSuspendFunctionClassSignature(arity: Int): IdSignature {
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
                functionClass, kFunctionClass -> listOf(r.symbol)
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
        IrFakeOverrideBuilder(typeSystem!!, FunctionTypeFakeOverrideBuilderStrategy(), emptyList())
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

