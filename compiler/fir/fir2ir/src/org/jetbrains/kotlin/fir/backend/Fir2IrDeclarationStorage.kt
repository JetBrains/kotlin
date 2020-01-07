/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyGetter
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertySetter
import org.jetbrains.kotlin.fir.descriptors.FirModuleDescriptor
import org.jetbrains.kotlin.fir.descriptors.FirPackageFragmentDescriptor
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.firProvider
import org.jetbrains.kotlin.fir.resolve.firSymbolProvider
import org.jetbrains.kotlin.fir.resolve.getOrPut
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.*
import org.jetbrains.kotlin.ir.descriptors.*
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrEnumConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrErrorExpressionImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrExpressionBodyImpl
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffsetSkippingComments

class Fir2IrDeclarationStorage(
    private val session: FirSession,
    private val irSymbolTable: SymbolTable,
    private val moduleDescriptor: FirModuleDescriptor,
    private val irBuiltIns: IrBuiltIns
) {
    private val firSymbolProvider = session.firSymbolProvider

    private val firProvider = session.firProvider

    private val fragmentCache = mutableMapOf<FqName, IrExternalPackageFragment>()

    private val fileCache = mutableMapOf<FirFile, IrFile>()

    private val classCache = mutableMapOf<FirRegularClass, IrClass>()

    private val typeParameterCache = mutableMapOf<FirTypeParameter, IrTypeParameter>()

    private val functionCache = mutableMapOf<FirSimpleFunction, IrSimpleFunction>()

    private val constructorCache = mutableMapOf<FirConstructor, IrConstructor>()

    private val propertyCache = mutableMapOf<FirProperty, IrProperty>()

    private val fieldCache = mutableMapOf<FirField, IrField>()

    private val enumEntryCache = mutableMapOf<FirEnumEntry, IrEnumEntry>()

    private val localStorage = Fir2IrLocalStorage()

    private val unitType = session.builtinTypes.unitType.toIrType(session, this)

    fun registerFile(firFile: FirFile, irFile: IrFile) {
        fileCache[firFile] = irFile
    }

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

    private fun FirTypeRef.toIrType(session: FirSession, declarationStorage: Fir2IrDeclarationStorage) =
        toIrType(session, declarationStorage, irBuiltIns)

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
        val receiverDescriptor = WrappedReceiverParameterDescriptor()
        thisReceiver = irSymbolTable.declareValueParameter(
            startOffset, endOffset, thisOrigin, receiverDescriptor, thisType
        ) { symbol ->
            IrValueParameterImpl(
                startOffset, endOffset, thisOrigin, symbol,
                Name.special("<this>"), -1, thisType,
                varargElementType = null, isCrossinline = false, isNoinline = false
            ).apply {
                this.parent = parent
                receiverDescriptor.bind(this)
            }
        }
        leaveScope(descriptor)
    }

    private fun IrClass.declareSupertypesAndTypeParameters(klass: FirClass<*>): IrClass {
        for (superTypeRef in klass.superTypeRefs) {
            superTypes += superTypeRef.toIrType(session, this@Fir2IrDeclarationStorage)
        }
        if (klass is FirRegularClass) {
            for ((index, typeParameter) in klass.typeParameters.withIndex()) {
                typeParameters += getIrTypeParameter(typeParameter, index).apply {
                    parent = this@declareSupertypesAndTypeParameters
                }
            }
        }
        return this
    }

    fun getIrClass(klass: FirClass<*>, setParent: Boolean = true): IrClass {
        val regularClass = klass as? FirRegularClass

        fun create(): IrClass {
            val descriptor = WrappedClassDescriptor()
            val origin = IrDeclarationOrigin.DEFINED
            val modality = regularClass?.modality ?: Modality.FINAL
            return klass.convertWithOffsets { startOffset, endOffset ->
                irSymbolTable.declareClass(startOffset, endOffset, origin, descriptor, modality) { symbol ->
                    IrClassImpl(
                        startOffset,
                        endOffset,
                        origin,
                        symbol,
                        regularClass?.name ?: Name.special("<anonymous>"),
                        klass.classKind,
                        regularClass?.visibility ?: Visibilities.LOCAL,
                        modality,
                        isCompanion = regularClass?.isCompanion == true,
                        isInner = regularClass?.isInner == true,
                        isData = regularClass?.isData == true,
                        isExternal = regularClass?.isExternal == true,
                        isInline = regularClass?.isInline == true,
                        isExpect = regularClass?.isExpect == true
                    ).apply {
                        descriptor.bind(this)
                        if (setParent && regularClass != null) {
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

        if (regularClass?.visibility == Visibilities.LOCAL || klass is FirAnonymousObject) {
            val cached = localStorage.getLocalClass(klass)
            if (cached != null) return cached
            val created = create()
            localStorage.putLocalClass(klass, created)
            created.declareSupertypesAndTypeParameters(klass)
            return created
        }
        // NB: klass can be either FirRegularClass or FirAnonymousObject
        return classCache.getOrPut(klass as FirRegularClass, { create() }) {
            it.declareSupertypesAndTypeParameters(klass)
        }
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
                    isCompanion = false, isInner = false, isData = false, isExternal = false, isInline = false, isExpect = false
                ).apply {
                    descriptor.bind(this)
                    declareThisReceiver()
                }
            }
        }.declareSupertypesAndTypeParameters(anonymousObject)
    }

    private fun getIrEnumEntryClass(enumEntry: FirEnumEntry, anonymousObject: FirAnonymousObject, irParent: IrClass?): IrClass {
        val descriptor = WrappedClassDescriptor()
        val origin = IrDeclarationOrigin.DEFINED
        val modality = Modality.FINAL
        return anonymousObject.convertWithOffsets { startOffset, endOffset ->
            irSymbolTable.declareClass(startOffset, endOffset, origin, descriptor, modality) { symbol ->
                IrClassImpl(
                    startOffset, endOffset, origin, symbol,
                    enumEntry.name, anonymousObject.classKind,
                    Visibilities.LOCAL, modality,
                    isCompanion = false, isInner = false, isData = false, isExternal = false, isInline = false, isExpect = false
                ).apply {
                    descriptor.bind(this)
                    declareThisReceiver()
                    if (irParent != null) {
                        this.parent = irParent
                    }
                }
            }
        }.declareSupertypesAndTypeParameters(anonymousObject)
    }

    private fun getIrTypeParameter(typeParameter: FirTypeParameter, index: Int = 0): IrTypeParameter {
        return typeParameterCache[typeParameter] ?: typeParameter.run {
            val descriptor = WrappedTypeParameterDescriptor()
            val origin = IrDeclarationOrigin.DEFINED
            val irTypeParameter =
                convertWithOffsets { startOffset, endOffset ->
                    irSymbolTable.declareGlobalTypeParameter(startOffset, endOffset, origin, descriptor) { symbol ->
                        IrTypeParameterImpl(
                            startOffset, endOffset, origin, symbol,
                            name, index,
                            isReified,
                            variance
                        ).apply {
                            descriptor.bind(this)
                        }
                    }
                }

            // Cache the type parameter BEFORE processing its bounds/supertypes, to properly handle recursive type bounds.
            typeParameterCache[typeParameter] = irTypeParameter
            bounds.mapTo(irTypeParameter.superTypes) { it.toIrType(session, this@Fir2IrDeclarationStorage) }
            irTypeParameter
        }
    }

    internal fun findIrParent(callableDeclaration: FirCallableDeclaration<*>): IrDeclarationParent? {
        val firBasedSymbol = callableDeclaration.symbol
        val callableId = firBasedSymbol.callableId
        val parentClassId = callableId.classId
        return if (parentClassId != null) {
            val parentFirSymbol = firSymbolProvider.getClassLikeSymbolByFqName(parentClassId)
            if (parentFirSymbol is FirClassSymbol) {
                val parentIrSymbol = getIrClassSymbol(parentFirSymbol)
                parentIrSymbol.owner
            } else {
                null
            }
        } else {
            val containerFile = firProvider.getFirCallableContainerFile(firBasedSymbol)
            if (containerFile != null) {
                fileCache[containerFile]
            } else {
                val packageFqName = callableId.packageName
                getIrExternalPackageFragment(packageFqName)
            }
        }
    }

    private fun IrDeclaration.setAndModifyParent(irParent: IrDeclarationParent?) {
        if (irParent != null) {
            parent = irParent
            if (irParent is IrExternalPackageFragment) {
                irParent.declarations += this
            } else if (irParent is IrClass) {
                // TODO: irParent.declarations += this (probably needed for external stuff)
            }
        }
    }

    private fun <T : IrFunction> T.declareDefaultSetterParameter(type: IrType): T {
        val parent = this
        val descriptor = WrappedValueParameterDescriptor()
        valueParameters += irSymbolTable.declareValueParameter(
            startOffset, endOffset, origin, descriptor, type
        ) { symbol ->
            IrValueParameterImpl(
                startOffset, endOffset, IrDeclarationOrigin.DEFINED, symbol,
                Name.special("<set-?>"), 0, type,
                varargElementType = null,
                isCrossinline = false, isNoinline = false
            ).apply {
                this.parent = parent
                descriptor.bind(this)
            }
        }
        return this
    }

    private fun <T : IrFunction> T.declareParameters(function: FirFunction<*>?, containingClass: IrClass?, isStatic: Boolean) {
        val parent = this
        if (function is FirDefaultPropertySetter) {
            val type = function.valueParameters.first().returnTypeRef.toIrType(session, this@Fir2IrDeclarationStorage)
            declareDefaultSetterParameter(type)
        } else if (function != null) {
            for ((index, valueParameter) in function.valueParameters.withIndex()) {
                valueParameters += createAndSaveIrParameter(valueParameter, index).apply { this.parent = parent }
            }
        }
        if (function !is FirConstructor) {
            val thisOrigin = IrDeclarationOrigin.DEFINED
            if (function is FirSimpleFunction) {
                for ((index, typeParameter) in function.typeParameters.withIndex()) {
                    typeParameters += getIrTypeParameter(typeParameter, index).apply { this.parent = parent }
                }
            }
            val receiverTypeRef = function?.receiverTypeRef
            if (receiverTypeRef != null) {
                extensionReceiverParameter = receiverTypeRef.convertWithOffsets { startOffset, endOffset ->
                    val type = receiverTypeRef.toIrType(session, this@Fir2IrDeclarationStorage)
                    val receiverDescriptor = WrappedReceiverParameterDescriptor()
                    irSymbolTable.declareValueParameter(
                        startOffset, endOffset, thisOrigin,
                        receiverDescriptor, type
                    ) { symbol ->
                        IrValueParameterImpl(
                            startOffset, endOffset, thisOrigin, symbol,
                            Name.special("<this>"), -1, type,
                            varargElementType = null, isCrossinline = false, isNoinline = false
                        ).apply {
                            this.parent = parent
                            receiverDescriptor.bind(this)
                        }
                    }
                }
            }
            if (containingClass != null && !isStatic) {
                val thisType = containingClass.thisReceiver!!.type
                val descriptor = WrappedReceiverParameterDescriptor()
                dispatchReceiverParameter = irSymbolTable.declareValueParameter(
                    startOffset, endOffset, thisOrigin, descriptor,
                    thisType
                ) { symbol ->
                    IrValueParameterImpl(
                        startOffset, endOffset, thisOrigin, symbol,
                        Name.special("<this>"), -1, thisType,
                        varargElementType = null, isCrossinline = false, isNoinline = false
                    ).apply {
                        this.parent = parent
                        descriptor.bind(this)
                    }
                }
            }
        }
    }

    private fun <T : IrFunction> T.bindAndDeclareParameters(
        function: FirFunction<*>?,
        descriptor: WrappedCallableDescriptor<T>,
        irParent: IrDeclarationParent?,
        isStatic: Boolean,
        shouldLeaveScope: Boolean
    ): T {
        descriptor.bind(this)
        enterScope(descriptor)
        declareParameters(function, containingClass = irParent as? IrClass, isStatic = isStatic)
        if (shouldLeaveScope) {
            leaveScope(descriptor)
        }
        return this
    }

    fun <T : IrFunction> T.enterLocalScope(function: FirFunction<*>): T {
        enterScope(descriptor)
        for ((firParameter, irParameter) in function.valueParameters.zip(valueParameters)) {
            irSymbolTable.introduceValueParameter(irParameter)
            localStorage.putParameter(firParameter, irParameter)
        }
        return this
    }

    fun getIrFunction(
        function: FirSimpleFunction,
        irParent: IrDeclarationParent? = null,
        shouldLeaveScope: Boolean = false,
        origin: IrDeclarationOrigin = IrDeclarationOrigin.DEFINED
    ): IrSimpleFunction {
        fun create(): IrSimpleFunction {
            val containerSource = function.containerSource
            val descriptor = containerSource?.let { WrappedFunctionDescriptorWithContainerSource(it) } ?: WrappedSimpleFunctionDescriptor()
            return function.convertWithOffsets { startOffset, endOffset ->
                enterScope(descriptor)
                val result = irSymbolTable.declareSimpleFunction(startOffset, endOffset, origin, descriptor) { symbol ->
                    IrFunctionImpl(
                        startOffset, endOffset, origin, symbol,
                        function.name, function.visibility, function.modality!!,
                        function.returnTypeRef.toIrType(session, this),
                        isInline = function.isInline,
                        isExternal = function.isExternal,
                        isTailrec = function.isTailRec,
                        isSuspend = function.isSuspend,
                        isExpect = function.isExpect,
                        isFakeOverride = origin == IrDeclarationOrigin.FAKE_OVERRIDE,
                        isOperator = function.isOperator
                    )
                }
                leaveScope(descriptor)
                result
            }.bindAndDeclareParameters(function, descriptor, irParent, isStatic = function.isStatic, shouldLeaveScope = shouldLeaveScope)
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

    fun getIrLocalFunction(
        function: FirAnonymousFunction,
        irParent: IrDeclarationParent? = null,
        shouldLeaveScope: Boolean = false
    ): IrSimpleFunction {
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
                    isSuspend = false,
                    isExpect = false,
                    isFakeOverride = false,
                    isOperator = false
                )
            }.bindAndDeclareParameters(
                function, descriptor, irParent = irParent, isStatic = false, shouldLeaveScope = shouldLeaveScope
            )
        }
    }

    fun getIrConstructor(
        constructor: FirConstructor,
        irParent: IrDeclarationParent? = null,
        shouldLeaveScope: Boolean = false
    ): IrConstructor {
        val cached = constructorCache[constructor]
        if (cached != null) {
            return if (shouldLeaveScope) cached else cached.enterLocalScope(constructor)
        }

        val descriptor = WrappedClassConstructorDescriptor()
        val origin = IrDeclarationOrigin.DEFINED
        val isPrimary = constructor.isPrimary
        val visibility = when {
            irParent is IrClass && irParent.modality == Modality.SEALED -> Visibilities.PRIVATE
            else -> constructor.visibility
        }
        val created = constructor.convertWithOffsets { startOffset, endOffset ->
            irSymbolTable.declareConstructor(startOffset, endOffset, origin, descriptor) { symbol ->
                IrConstructorImpl(
                    startOffset, endOffset, origin, symbol,
                    constructor.name, visibility,
                    constructor.returnTypeRef.toIrType(session, this),
                    isInline = false, isExternal = false, isPrimary = isPrimary, isExpect = false
                ).bindAndDeclareParameters(constructor, descriptor, irParent, isStatic = true, shouldLeaveScope = shouldLeaveScope)
            }
        }
        constructorCache[constructor] = created
        return created
    }

    private fun createIrPropertyAccessor(
        propertyAccessor: FirPropertyAccessor?,
        correspondingProperty: IrProperty,
        propertyType: IrType,
        irParent: IrDeclarationParent?,
        isSetter: Boolean,
        origin: IrDeclarationOrigin,
        startOffset: Int,
        endOffset: Int
    ): IrSimpleFunction {
        val descriptor = WrappedSimpleFunctionDescriptor()
        val prefix = if (isSetter) "set" else "get"
        return irSymbolTable.declareSimpleFunction(
            propertyAccessor?.psi?.startOffsetSkippingComments ?: startOffset,
            propertyAccessor?.psi?.endOffset ?: endOffset,
            origin, descriptor
        ) { symbol ->
            val accessorReturnType = if (isSetter) unitType else propertyType
            IrFunctionImpl(
                startOffset, endOffset, origin, symbol,
                Name.special("<$prefix-${correspondingProperty.name}>"),
                propertyAccessor?.visibility ?: correspondingProperty.visibility,
                correspondingProperty.modality, accessorReturnType,
                isInline = false, isExternal = false, isTailrec = false, isSuspend = false, isExpect = false,
                isFakeOverride = origin == IrDeclarationOrigin.FAKE_OVERRIDE,
                isOperator = false
            ).apply {
                if (propertyAccessor == null && isSetter) {
                    declareDefaultSetterParameter(propertyType)
                }
            }.bindAndDeclareParameters(
                propertyAccessor, descriptor, irParent, isStatic = irParent !is IrClass, shouldLeaveScope = true
            ).apply {
                if (irParent != null) {
                    parent = irParent
                }
                correspondingPropertySymbol = correspondingProperty.symbol
            }
        }
    }


    fun getIrEnumEntry(
        enumEntry: FirEnumEntry,
        irParent: IrClass? = null,
        origin: IrDeclarationOrigin = IrDeclarationOrigin.DEFINED
    ): IrEnumEntry {
        return enumEntryCache.getOrPut(enumEntry) {
            enumEntry.convertWithOffsets { startOffset, endOffset ->
                val desc = WrappedEnumEntryDescriptor()
                enterScope(desc)
                val result = irSymbolTable.declareEnumEntry(startOffset, endOffset, origin, desc) { symbol ->
                    IrEnumEntryImpl(
                        startOffset, endOffset, origin, symbol, enumEntry.name
                    ).apply {
                        desc.bind(this)
                        val irType = enumEntry.returnTypeRef.toIrType(session, this@Fir2IrDeclarationStorage)
                        if (irParent != null) {
                            this.parent = irParent
                        }
                        val initializer = enumEntry.initializer
                        if (initializer != null) {
                            initializer as FirAnonymousObject
                            val klass = getIrEnumEntryClass(enumEntry, initializer, irParent)

                            this.correspondingClass = klass
                        } else if (irParent != null) {
                            this.initializerExpression =
                                IrEnumConstructorCallImpl(startOffset, endOffset, irType, irParent.constructors.first().symbol)
                        }
                    }
                }
                leaveScope(desc)
                result
            }
        }
    }

    fun getIrProperty(
        property: FirProperty,
        irParent: IrDeclarationParent? = null,
        origin: IrDeclarationOrigin = IrDeclarationOrigin.DEFINED
    ): IrProperty {
        return propertyCache.getOrPut(property) {
            val containerSource = property.containerSource
            val descriptor = containerSource?.let { WrappedPropertyDescriptorWithContainerSource(it) } ?: WrappedPropertyDescriptor()
            property.convertWithOffsets { startOffset, endOffset ->
                enterScope(descriptor)
                val result = irSymbolTable.declareProperty(
                    startOffset, endOffset,
                    origin, descriptor, property.delegate != null
                ) { symbol ->
                    IrPropertyImpl(
                        startOffset, endOffset, origin, symbol,
                        property.name, property.visibility, property.modality!!,
                        isVar = property.isVar,
                        isConst = property.isConst,
                        isLateinit = property.isLateInit,
                        isDelegated = property.delegate != null,
                        // TODO
                        isExternal = false,
                        isExpect = property.isExpect,
                        isFakeOverride = origin == IrDeclarationOrigin.FAKE_OVERRIDE
                    ).apply {
                        descriptor.bind(this)
                        val type = property.returnTypeRef.toIrType(session, this@Fir2IrDeclarationStorage)
                        getter = createIrPropertyAccessor(
                            property.getter, this, type, irParent, false,
                            when {
                                property.delegate != null -> IrDeclarationOrigin.DELEGATED_PROPERTY_ACCESSOR
                                property.getter is FirDefaultPropertyGetter -> IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR
                                else -> origin
                            },
                            startOffset, endOffset
                        )
                        if (property.isVar) {
                            setter = createIrPropertyAccessor(
                                property.setter, this, type, irParent, true,
                                when {
                                    property.delegate != null -> IrDeclarationOrigin.DELEGATED_PROPERTY_ACCESSOR
                                    property.setter is FirDefaultPropertySetter -> IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR
                                    else -> origin
                                },
                                startOffset, endOffset
                            )
                        }
                    }
                }
                leaveScope(descriptor)
                result
            }
        }
    }

    private fun getIrField(field: FirField): IrField {
        return fieldCache.getOrPut(field) {
            val descriptor = WrappedFieldDescriptor()
            val origin = IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB
            val type = field.returnTypeRef.toIrType(session, this)
            field.convertWithOffsets { startOffset, endOffset ->
                irSymbolTable.declareField(
                    startOffset, endOffset,
                    origin, descriptor, type
                ) { symbol ->
                    IrFieldImpl(
                        startOffset, endOffset, origin, symbol,
                        field.name, type, field.visibility,
                        isFinal = field.modality == Modality.FINAL,
                        isExternal = false,
                        isStatic = field.isStatic,
                        isFakeOverride = false
                    ).apply {
                        descriptor.bind(this)
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
                    if (valueParameter.defaultValue != null) {
                        this.defaultValue = IrExpressionBodyImpl(
                            IrErrorExpressionImpl(
                                UNDEFINED_OFFSET, UNDEFINED_OFFSET, type,
                                "Stub expression for default value of ${valueParameter.name}"
                            )
                        )
                    }
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

    fun createAndSaveIrVariable(variable: FirVariable<*>): IrVariable {
        val type = variable.returnTypeRef.toIrType(session, this)
        // Some temporary variables are produced in RawFirBuilder, but we consistently use special names for them.
        val origin =
            if (variable.name.isSpecial) IrDeclarationOrigin.IR_TEMPORARY_VARIABLE else IrDeclarationOrigin.DEFINED
        val irVariable = variable.convertWithOffsets { startOffset, endOffset ->
            declareIrVariable(
                startOffset, endOffset, origin,
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
        ).apply {
            initializer = base
        }
    }

    fun getIrClassSymbol(firClassSymbol: FirClassSymbol<*>): IrClassSymbol {
        val irClass = getIrClass(firClassSymbol.fir)
        return irSymbolTable.referenceClass(irClass.descriptor)
    }

    fun getIrTypeParameterSymbol(firTypeParameterSymbol: FirTypeParameterSymbol): IrTypeParameterSymbol {
        val irTypeParameter = getIrTypeParameter(firTypeParameterSymbol.fir)
        return irSymbolTable.referenceTypeParameter(irTypeParameter.descriptor)
    }

    fun getIrFunctionSymbol(firFunctionSymbol: FirFunctionSymbol<*>): IrFunctionSymbol {
        val firDeclaration = firFunctionSymbol.fir
        val irParent = (firDeclaration as? FirCallableDeclaration<*>)?.let { findIrParent(it) }
        return when (firDeclaration) {
            is FirSimpleFunction -> {
                val irDeclaration = getIrFunction(firDeclaration, irParent, shouldLeaveScope = true).apply {
                    setAndModifyParent(irParent)
                }
                irSymbolTable.referenceSimpleFunction(irDeclaration.descriptor)
            }
            is FirAnonymousFunction -> {
                val irDeclaration = getIrLocalFunction(firDeclaration, irParent, shouldLeaveScope = true).apply {
                    setAndModifyParent(irParent)
                }
                irSymbolTable.referenceSimpleFunction(irDeclaration.descriptor)
            }
            is FirConstructor -> {
                val irDeclaration = getIrConstructor(firDeclaration, irParent, shouldLeaveScope = true).apply {
                    setAndModifyParent(irParent)
                }
                irSymbolTable.referenceConstructor(irDeclaration.descriptor)
            }
            else -> throw AssertionError("Should not be here: ${firDeclaration::class.java}: ${firDeclaration.render()}")
        }
    }

    fun getIrPropertyOrFieldSymbol(firVariableSymbol: FirVariableSymbol<*>): IrSymbol {
        return when (val fir = firVariableSymbol.fir) {
            is FirProperty -> {
                val irParent = findIrParent(fir)
                val irProperty = getIrProperty(fir, irParent).apply {
                    setAndModifyParent(irParent)
                }
                irSymbolTable.referenceProperty(irProperty.descriptor)
            }
            is FirField -> {
                val irField = getIrField(fir).apply {
                    setAndModifyParent(findIrParent(fir))
                }
                irSymbolTable.referenceField(irField.descriptor)
            }
            else -> throw IllegalArgumentException("Unexpected fir in property symbol: ${fir.render()}")
        }
    }

    fun getIrBackingFieldSymbol(firVariableSymbol: FirVariableSymbol<*>): IrSymbol {
        return when (val fir = firVariableSymbol.fir) {
            is FirProperty -> {
                val irProperty = getIrProperty(fir).apply {
                    setAndModifyParent(findIrParent(fir))
                }
                irSymbolTable.referenceField(irProperty.backingField!!.descriptor)
            }
            else -> {
                getIrVariableSymbol(fir)
            }
        }
    }

    private fun getIrVariableSymbol(firVariable: FirVariable<*>): IrVariableSymbol {
        val irDeclaration = localStorage.getVariable(firVariable)
            ?: throw IllegalArgumentException("Cannot find variable ${firVariable.render()} in local storage")
        return irSymbolTable.referenceVariable(irDeclaration.descriptor)
    }

    fun getIrValueSymbol(firVariableSymbol: FirVariableSymbol<*>): IrSymbol {
        return when (val firDeclaration = firVariableSymbol.fir) {
            is FirEnumEntry -> {
                val irEnumEntry = getIrEnumEntry(firDeclaration)
                irSymbolTable.referenceEnumEntry(irEnumEntry.descriptor)
            }
            is FirValueParameter -> {
                val irDeclaration = localStorage.getParameter(firDeclaration)
                // catch parameter is FirValueParameter in FIR but IrVariable in IR
                    ?: return getIrVariableSymbol(firDeclaration)
                irSymbolTable.referenceValueParameter(irDeclaration.descriptor)
            }
            else -> {
                getIrVariableSymbol(firDeclaration)
            }
        }
    }
}