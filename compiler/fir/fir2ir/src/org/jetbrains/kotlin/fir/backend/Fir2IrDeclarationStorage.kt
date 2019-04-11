/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.backend.common.descriptors.*
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.descriptors.FirModuleDescriptor
import org.jetbrains.kotlin.fir.descriptors.FirPackageFragmentDescriptor
import org.jetbrains.kotlin.fir.expressions.FirVariable
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.FirSymbolProvider
import org.jetbrains.kotlin.fir.service
import org.jetbrains.kotlin.fir.symbols.LibraryTypeParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.*
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.types.Variance

class Fir2IrDeclarationStorage(
    private val session: FirSession,
    private val irSymbolTable: SymbolTable,
    private val moduleDescriptor: FirModuleDescriptor
) {
    private val firSymbolProvider = session.service<FirSymbolProvider>()

    private val fragmentCache = mutableMapOf<FqName, IrExternalPackageFragment>()

    private val classCache = mutableMapOf<FirRegularClass, IrClass>()

    private val typeParameterCache = mutableMapOf<FirTypeParameter, IrTypeParameter>()

    private val libraryTypeParameterCache = mutableMapOf<LibraryTypeParameterSymbol, IrTypeParameter>()

    private val functionCache = mutableMapOf<FirNamedFunction, IrSimpleFunction>()

    private val constructorCache = mutableMapOf<FirConstructor, IrConstructor>()

    private val propertyCache = mutableMapOf<FirProperty, IrProperty>()

    private val localStorage = Fir2IrLocalStorage()

    fun enterScope(descriptor: DeclarationDescriptor) {
        irSymbolTable.enterScope(descriptor)
        if (descriptor is WrappedSimpleFunctionDescriptor ||
            descriptor is WrappedClassConstructorDescriptor ||
            descriptor is WrappedPropertyDescriptor
        ) {
            localStorage.enterCallable()
        }
    }

    fun leaveScope(descriptor: DeclarationDescriptor) {
        if (descriptor is WrappedSimpleFunctionDescriptor ||
            descriptor is WrappedClassConstructorDescriptor ||
            descriptor is WrappedPropertyDescriptor
        ) {
            localStorage.leaveCallable()
        }
        irSymbolTable.leaveScope(descriptor)
    }

    private fun getIrExternalPackageFragment(fqName: FqName): IrExternalPackageFragment {
        return fragmentCache.getOrPut(fqName) {
            // TODO: module descriptor is wrong here
            return irSymbolTable.declareExternalPackageFragment(FirPackageFragmentDescriptor(fqName, moduleDescriptor))
        }
    }

    private fun IrClass.declareThisReceiver() {
        enterScope(descriptor)
        val thisOrigin = IrDeclarationOrigin.INSTANCE_RECEIVER
        val thisType = IrSimpleTypeImpl(symbol, false, emptyList(), emptyList())
        val parent = this
        thisReceiver = irSymbolTable.declareValueParameter(
            startOffset, endOffset, thisOrigin, WrappedValueParameterDescriptor(), thisType
        ) { symbol ->
            IrValueParameterImpl(
                startOffset, endOffset, thisOrigin, symbol,
                Name.special("<this>"), -1, thisType,
                varargElementType = null, isCrossinline = false, isNoinline = false
            ).apply { this.parent = parent }
        }
        leaveScope(descriptor)
    }

    fun getIrClass(regularClass: FirRegularClass, setParent: Boolean = true): IrClass {
        fun create(): IrClass {
            val descriptor = WrappedClassDescriptor()
            val origin = IrDeclarationOrigin.DEFINED
            val modality = regularClass.modality!!
            return regularClass.convertWithOffsets { startOffset, endOffset ->
                irSymbolTable.declareClass(startOffset, endOffset, origin, descriptor, modality) { symbol ->
                    IrClassImpl(
                        startOffset, endOffset, origin, symbol,
                        regularClass.name, regularClass.classKind,
                        regularClass.visibility, modality,
                        regularClass.isCompanion, regularClass.isInner,
                        regularClass.isData, false, regularClass.isInline
                    ).apply {
                        descriptor.bind(this)
                        if (setParent) {
                            val classId = regularClass.classId
                            val parentId = classId.outerClassId
                            if (parentId != null) {
                                val parentFirSymbol = firSymbolProvider.getClassLikeSymbolByFqName(parentId)
                                if (parentFirSymbol is FirClassSymbol) {
                                    val parentIrSymbol = getIrClassSymbol(parentFirSymbol)
                                    parent = parentIrSymbol.owner
                                }
                            } else {
                                val packageFqName = classId.packageFqName
                                parent = getIrExternalPackageFragment(packageFqName)
                            }
                        }
                        declareThisReceiver()
                    }
                }
            }
        }

        if (regularClass.visibility == Visibilities.LOCAL) {
            val cached = localStorage.getLocalClass(regularClass)
            if (cached != null) return cached
            val created = create()
            localStorage.putLocalClass(regularClass, created)
            return created
        }
        return classCache.getOrPut(regularClass, ::create)
    }

    fun getIrAnonymousObject(anonymousObject: FirAnonymousObject): IrClass {
        val descriptor = WrappedClassDescriptor()
        val origin = IrDeclarationOrigin.DEFINED
        val modality = Modality.FINAL
        return anonymousObject.convertWithOffsets { startOffset, endOffset ->
            irSymbolTable.declareClass(startOffset, endOffset, origin, descriptor, modality) { symbol ->
                IrClassImpl(
                    startOffset, endOffset, origin, symbol,
                    Name.special("<no name provided>"), anonymousObject.classKind,
                    Visibilities.LOCAL, modality,
                    isCompanion = false, isInner = false, isData = false, isExternal = false, isInline = false
                ).apply {
                    descriptor.bind(this)
                    declareThisReceiver()
                }
            }
        }
    }

    fun getIrTypeParameter(typeParameter: FirTypeParameter, index: Int = 0): IrTypeParameter {
        return typeParameterCache.getOrPut(typeParameter) {
            val descriptor = WrappedTypeParameterDescriptor()
            val origin = IrDeclarationOrigin.DEFINED
            typeParameter.convertWithOffsets { startOffset, endOffset ->
                irSymbolTable.declareGlobalTypeParameter(startOffset, endOffset, origin, descriptor) { symbol ->
                    IrTypeParameterImpl(
                        startOffset, endOffset, origin, symbol,
                        typeParameter.name, index,
                        typeParameter.isReified,
                        typeParameter.variance
                    ).apply {
                        descriptor.bind(this)
                    }
                }
            }
        }
    }

    private fun IrDeclaration.setParentByOwnFir(firMember: FirCallableMemberDeclaration) {
        val firBasedSymbol = firMember.symbol
        val callableId = firBasedSymbol.callableId
        val parentClassId = callableId.classId
        if (parentClassId != null) {
            val parentFirSymbol = firSymbolProvider.getClassLikeSymbolByFqName(parentClassId)
            if (parentFirSymbol is FirClassSymbol) {
                val parentIrSymbol = getIrClassSymbol(parentFirSymbol)
                val parentIrClass = parentIrSymbol.owner
                parent = parentIrClass
                // TODO: parentIrClass.declarations += this (probably needed for external stuff)
            }
        } else {
            val packageFqName = callableId.packageName
            val parentIrPackageFragment = getIrExternalPackageFragment(packageFqName)
            parent = parentIrPackageFragment
            parentIrPackageFragment.declarations += this
        }
    }

    fun <T : IrFunction> T.declareParameters(function: FirFunction) {
        val parent = this
        for ((index, valueParameter) in function.valueParameters.withIndex()) {
            valueParameters += createAndSaveIrParameter(valueParameter, index).apply { this.parent = parent }
        }
    }

    private fun <T : IrFunction> T.bindAndDeclareParameters(
        function: FirFunction,
        descriptor: WrappedCallableDescriptor<T>,
        setParent: Boolean,
        shouldLeaveScope: Boolean
    ): T {
        descriptor.bind(this)
        if (setParent) {
            setParentByOwnFir(function as FirCallableMemberDeclaration)
        }
        enterScope(descriptor)
        declareParameters(function)
        if (shouldLeaveScope) {
            leaveScope(descriptor)
        }
        return this
    }

    private fun <T : IrFunction> T.enterLocalScope(function: FirFunction): T {
        enterScope(descriptor)
        for ((firParameter, irParameter) in function.valueParameters.zip(valueParameters)) {
            irSymbolTable.introduceValueParameter(irParameter)
            localStorage.putParameter(firParameter, irParameter)
        }
        return this
    }

    fun getIrFunction(
        function: FirNamedFunction,
        setParent: Boolean = true,
        shouldLeaveScope: Boolean = false,
        origin: IrDeclarationOrigin = IrDeclarationOrigin.DEFINED
    ): IrSimpleFunction {
        fun create(): IrSimpleFunction {
            val descriptor = WrappedSimpleFunctionDescriptor()
            return function.convertWithOffsets { startOffset, endOffset ->
                irSymbolTable.declareSimpleFunction(startOffset, endOffset, origin, descriptor) { symbol ->
                    IrFunctionImpl(
                        startOffset, endOffset, origin, symbol,
                        function.name, function.visibility, function.modality!!,
                        function.returnTypeRef.toIrType(session, this),
                        function.isInline, function.isExternal,
                        function.isTailRec, function.isSuspend
                    )
                }
            }.bindAndDeclareParameters(function, descriptor, setParent, shouldLeaveScope)
        }

        if (function.visibility == Visibilities.LOCAL) {
            val cached = localStorage.getLocalFunction(function)
            if (cached != null) {
                return if (shouldLeaveScope) cached else cached.enterLocalScope(function)
            }
            val created = create()
            localStorage.putLocalFunction(function, created)
            return created
        }
        val cached = functionCache[function]
        if (cached != null) {
            return if (shouldLeaveScope) cached else cached.enterLocalScope(function)
        }
        val created = create()
        functionCache[function] = created
        return created
    }

    fun getIrLocalFunction(function: FirAnonymousFunction): IrSimpleFunction {
        val descriptor = WrappedSimpleFunctionDescriptor()
        val isLambda = function.psi is KtFunctionLiteral
        val origin = if (isLambda) IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA else IrDeclarationOrigin.DEFINED
        return function.convertWithOffsets { startOffset, endOffset ->
            irSymbolTable.declareSimpleFunction(startOffset, endOffset, origin, descriptor) { symbol ->
                IrFunctionImpl(
                    startOffset, endOffset, origin, symbol,
                    if (isLambda) Name.special("<anonymous>") else Name.special("<no name provided>"),
                    Visibilities.LOCAL, Modality.FINAL,
                    function.returnTypeRef.toIrType(session, this),
                    isInline = false, isExternal = false, isTailrec = false,
                    // TODO: suspend lambda
                    isSuspend = false
                )
            }.bindAndDeclareParameters(function, descriptor, setParent = false, shouldLeaveScope = false)
        }
    }

    fun getIrConstructor(constructor: FirConstructor, setParent: Boolean = true, shouldLeaveScope: Boolean = false): IrConstructor {
        return constructorCache.getOrPut(constructor) {
            val descriptor = WrappedClassConstructorDescriptor()
            val origin = IrDeclarationOrigin.DEFINED
            val isPrimary = constructor.isPrimary
            return constructor.convertWithOffsets { startOffset, endOffset ->
                irSymbolTable.declareConstructor(startOffset, endOffset, origin, descriptor) { symbol ->
                    IrConstructorImpl(
                        startOffset, endOffset, origin, symbol,
                        constructor.name, constructor.visibility,
                        constructor.returnTypeRef.toIrType(session, this),
                        false, false, isPrimary
                    ).bindAndDeclareParameters(constructor, descriptor, setParent, shouldLeaveScope)
                }
            }

        }
    }

    fun getIrProperty(property: FirProperty, setParent: Boolean = true): IrProperty {
        return propertyCache.getOrPut(property) {
            val descriptor = WrappedPropertyDescriptor()
            val origin = IrDeclarationOrigin.DEFINED
            property.convertWithOffsets { startOffset, endOffset ->
                irSymbolTable.declareProperty(
                    startOffset, endOffset,
                    origin, descriptor, property.delegate != null
                ) { symbol ->
                    IrPropertyImpl(
                        startOffset, endOffset, origin, symbol,
                        property.name, property.visibility, property.modality!!,
                        property.isVar, property.isConst, property.isLateInit,
                        property.delegate != null,
                        // TODO
                        isExternal = false
                    ).apply {
                        descriptor.bind(this)
                        if (setParent) {
                            setParentByOwnFir(property)
                        }
                    }
                }
            }
        }
    }

    private fun createAndSaveIrParameter(valueParameter: FirValueParameter, index: Int = -1): IrValueParameter {
        val descriptor = WrappedValueParameterDescriptor()
        val origin = IrDeclarationOrigin.DEFINED
        val type = valueParameter.returnTypeRef.toIrType(session, this)
        val irParameter = valueParameter.convertWithOffsets { startOffset, endOffset ->
            irSymbolTable.declareValueParameter(
                startOffset, endOffset, origin, descriptor, type
            ) { symbol ->
                IrValueParameterImpl(
                    startOffset, endOffset, origin, symbol,
                    valueParameter.name, index, type,
                    null, valueParameter.isCrossinline, valueParameter.isNoinline
                ).apply {
                    descriptor.bind(this)
                }
            }
        }
        localStorage.putParameter(valueParameter, irParameter)
        return irParameter
    }

    private var lastTemporaryIndex: Int = 0
    private fun nextTemporaryIndex(): Int = lastTemporaryIndex++

    private fun getNameForTemporary(nameHint: String?): String {
        val index = nextTemporaryIndex()
        return if (nameHint != null) "tmp${index}_$nameHint" else "tmp$index"
    }

    private fun declareIrVariable(
        startOffset: Int, endOffset: Int,
        origin: IrDeclarationOrigin, name: Name, type: IrType,
        isVar: Boolean, isConst: Boolean, isLateinit: Boolean
    ): IrVariable {
        val descriptor = WrappedVariableDescriptor()
        return irSymbolTable.declareVariable(startOffset, endOffset, origin, descriptor, type) { symbol ->
            IrVariableImpl(
                startOffset, endOffset, origin, symbol, name, type,
                isVar, isConst, isLateinit
            ).apply {
                descriptor.bind(this)
            }
        }
    }

    fun createAndSaveIrVariable(variable: FirVariable): IrVariable {
        val type = variable.returnTypeRef.toIrType(session, this)
        val irVariable = variable.convertWithOffsets { startOffset, endOffset ->
            declareIrVariable(
                startOffset, endOffset, IrDeclarationOrigin.DEFINED,
                variable.name, type, variable.isVar, isConst = false, isLateinit = false
            )
        }
        localStorage.putVariable(variable, irVariable)
        return irVariable
    }

    fun declareTemporaryVariable(base: IrExpression, nameHint: String? = null): IrVariable {
        return declareIrVariable(
            base.startOffset, base.endOffset, IrDeclarationOrigin.IR_TEMPORARY_VARIABLE,
            Name.identifier(getNameForTemporary(nameHint)), base.type,
            isVar = false, isConst = false, isLateinit = false
        )
    }

    fun getIrClassSymbol(firClassSymbol: FirClassSymbol): IrClassSymbol {
        val irClass = getIrClass(firClassSymbol.fir)
        return irSymbolTable.referenceClass(irClass.descriptor)
    }

    fun getIrTypeParameterSymbol(firTypeParameterSymbol: FirTypeParameterSymbol): IrTypeParameterSymbol {
        val irTypeParameter = getIrTypeParameter(firTypeParameterSymbol.fir)
        return irSymbolTable.referenceTypeParameter(irTypeParameter.descriptor)
    }

    fun getIrTypeParameterSymbol(typeParameterSymbol: LibraryTypeParameterSymbol): IrTypeParameterSymbol {
        val irTypeParameter = libraryTypeParameterCache.getOrPut(typeParameterSymbol) {
            val descriptor = WrappedTypeParameterDescriptor()
            val origin = IrDeclarationOrigin.DEFINED
            irSymbolTable.declareGlobalTypeParameter(-1, -1, origin, descriptor) { symbol ->
                IrTypeParameterImpl(
                    -1, -1, origin, symbol,
                    typeParameterSymbol.name, -1,
                    false,
                    Variance.INVARIANT
                ).apply {
                    descriptor.bind(this)
                }
            }
        }
        return irSymbolTable.referenceTypeParameter(irTypeParameter.descriptor)
    }

    fun getIrFunctionSymbol(firFunctionSymbol: FirFunctionSymbol): IrFunctionSymbol {
        return when (val firDeclaration = firFunctionSymbol.fir) {
            is FirNamedFunction -> {
                val irDeclaration = getIrFunction(firDeclaration, shouldLeaveScope = true)
                irSymbolTable.referenceSimpleFunction(irDeclaration.descriptor)
            }
            is FirConstructor -> {
                val irDeclaration = getIrConstructor(firDeclaration, shouldLeaveScope = true)
                irSymbolTable.referenceConstructor(irDeclaration.descriptor)
            }
            else -> throw AssertionError("Should not be here")
        }
    }

    fun getIrPropertySymbol(firPropertySymbol: FirPropertySymbol): IrPropertySymbol {
        val irProperty = getIrProperty(firPropertySymbol.fir as FirProperty)
        return irSymbolTable.referenceProperty(irProperty.descriptor)
    }

    private fun getIrVariableSymbol(firVariable: FirVariable): IrVariableSymbol {
        val irDeclaration = localStorage.getVariable(firVariable)
            ?: throw IllegalArgumentException("Cannot find variable ${firVariable.render()} in local storage")
        return irSymbolTable.referenceVariable(irDeclaration.descriptor)
    }

    fun getIrValueSymbol(firVariableSymbol: FirVariableSymbol): IrValueSymbol {
        return when (val firDeclaration = firVariableSymbol.fir) {
            is FirValueParameter -> {
                val irDeclaration = localStorage.getParameter(firDeclaration)
                // catch parameter is FirValueParameter in FIR but IrVariable in IR
                    ?: return getIrVariableSymbol(firDeclaration)
                irSymbolTable.referenceValueParameter(irDeclaration.descriptor)
            }
            is FirVariable -> {
                getIrVariableSymbol(firDeclaration)
            }
            else -> throw AssertionError("Should not be here")
        }
    }
}