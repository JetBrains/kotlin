/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyGetter
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertySetter
import org.jetbrains.kotlin.fir.descriptors.FirModuleDescriptor
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirElseIfTrueCondition
import org.jetbrains.kotlin.fir.expressions.impl.FirNoReceiverExpression
import org.jetbrains.kotlin.fir.expressions.impl.FirUnitExpression
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.impl.FirPropertyFromParameterResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.buildUseSiteMemberScope
import org.jetbrains.kotlin.fir.resolve.calls.SyntheticPropertySymbol
import org.jetbrains.kotlin.fir.resolve.firSymbolProvider
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.resolve.transformers.IntegerLiteralTypeApproximationTransformer
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.scopes.impl.FirClassSubstitutionScope
import org.jetbrains.kotlin.fir.scopes.impl.FirIntegerOperator
import org.jetbrains.kotlin.fir.symbols.AccessorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitor
import org.jetbrains.kotlin.fir.visitors.transformSingle
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFieldImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.descriptors.WrappedValueParameterDescriptor
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi2ir.PsiSourceManager
import org.jetbrains.kotlin.types.AbstractStrictEqualityTypeChecker
import java.util.*

class Fir2IrVisitor(
    private val session: FirSession,
    private val moduleDescriptor: FirModuleDescriptor,
    private val symbolTable: SymbolTable,
    private val sourceManager: PsiSourceManager,
    override val irBuiltIns: IrBuiltIns,
    private val fakeOverrideMode: FakeOverrideMode
) : FirDefaultVisitor<IrElement, Any?>(), IrGeneratorContextInterface {
    companion object {
        private val NEGATED_OPERATIONS: Set<FirOperation> = EnumSet.of(FirOperation.NOT_EQ, FirOperation.NOT_IDENTITY)

        private val UNARY_OPERATIONS: Set<FirOperation> = EnumSet.of(FirOperation.EXCL)
    }

    private val integerApproximator = IntegerLiteralTypeApproximationTransformer(session.firSymbolProvider, session.inferenceContext)

    private val typeContext = session.typeContext

    private val declarationStorage = Fir2IrDeclarationStorage(session, symbolTable, moduleDescriptor, irBuiltIns)

    private val nothingType = session.builtinTypes.nothingType.toIrType(session, declarationStorage)

    private val unitType = session.builtinTypes.unitType.toIrType(session, declarationStorage)

    private val booleanType = session.builtinTypes.booleanType.toIrType(session, declarationStorage)

    private val stringType = session.builtinTypes.stringType.toIrType(session, declarationStorage)

    private fun ModuleDescriptor.findPackageFragmentForFile(file: FirFile): PackageFragmentDescriptor =
        getPackage(file.packageFqName).fragments.first()

    private val parentStack = mutableListOf<IrDeclarationParent>()

    private fun <T : IrDeclarationParent> T.withParent(f: T.() -> Unit): T {
        parentStack += this
        f()
        parentStack.removeAt(parentStack.size - 1)
        return this
    }

    private fun <T : IrDeclaration> T.setParentByParentStack(): T {
        this.parent = parentStack.last()
        return this
    }

    private val functionStack = mutableListOf<IrFunction>()

    private fun <T : IrFunction> T.withFunction(f: T.() -> Unit): T {
        functionStack += this
        f()
        functionStack.removeAt(functionStack.size - 1)
        return this
    }

    private val propertyStack = mutableListOf<IrProperty>()

    private fun IrProperty.withProperty(f: IrProperty.() -> Unit): IrProperty {
        propertyStack += this
        f()
        propertyStack.removeAt(propertyStack.size - 1)
        return this
    }

    private val classStack = mutableListOf<IrClass>()

    private fun IrClass.withClass(f: IrClass.() -> Unit): IrClass {
        classStack += this
        f()
        classStack.removeAt(classStack.size - 1)
        return this
    }

    private val subjectVariableStack = mutableListOf<IrVariable>()

    private fun <T> IrVariable?.withSubject(f: () -> T): T {
        if (this != null) subjectVariableStack += this
        val result = f()
        if (this != null) subjectVariableStack.removeAt(subjectVariableStack.size - 1)
        return result
    }

    private fun FirTypeRef.toIrType(session: FirSession, declarationStorage: Fir2IrDeclarationStorage) =
        toIrType(session, declarationStorage, irBuiltIns)

    override fun visitElement(element: FirElement, data: Any?): IrElement {
        TODO("Should not be here: ${element.render()}")
    }

    override fun visitFile(file: FirFile, data: Any?): IrFile {
        return IrFileImpl(
            sourceManager.getOrCreateFileEntry(file.psi as KtFile),
            moduleDescriptor.findPackageFragmentForFile(file)
        ).withParent {
            declarationStorage.registerFile(file, this)
            file.declarations.forEach {
                val irDeclaration = it.toIrDeclaration() ?: return@forEach
                declarations += irDeclaration
            }

            file.annotations.forEach {
                val irCall = it.accept(this@Fir2IrVisitor, data) as? IrConstructorCall ?: return@forEach
                annotations += irCall
            }
        }
    }

    private fun FirDeclaration.toIrDeclaration(): IrDeclaration? {
        if (this is FirTypeAlias) return null
        return accept(this@Fir2IrVisitor, null) as IrDeclaration
    }

    override fun visitEnumEntry(enumEntry: FirEnumEntry, data: Any?): IrElement {
        val irEnumEntry = declarationStorage.getIrEnumEntry(enumEntry, irParent = parentStack.last() as IrClass)
        irEnumEntry.correspondingClass?.withParent {
            setClassContent(enumEntry.initializer as FirAnonymousObject)
        }
        //irEnumEntry.initializerExpression = IrEnumConstructorCallImpl()
        return irEnumEntry.setParentByParentStack()
    }

    private fun FirTypeRef.collectCallableNamesFromThisAndSupertypes(result: MutableList<Name> = mutableListOf()): List<Name> {
        if (this is FirResolvedTypeRef) {
            val superType = type
            if (superType is ConeClassLikeType) {
                when (val superSymbol = superType.lookupTag.toSymbol(this@Fir2IrVisitor.session)) {
                    is FirClassSymbol -> {
                        val superClass = superSymbol.fir as FirClass<*>
                        for (declaration in superClass.declarations) {
                            if (declaration is FirMemberDeclaration && (declaration is FirSimpleFunction || declaration is FirProperty)) {
                                result += declaration.name
                            }
                        }
                        superClass.collectCallableNamesFromSupertypes(result)
                    }
                    is FirTypeAliasSymbol -> {
                        val superAlias = superSymbol.fir
                        superAlias.expandedTypeRef.collectCallableNamesFromThisAndSupertypes(result)
                    }
                }
            }
        }
        return result
    }

    private fun FirClass<*>.collectCallableNamesFromSupertypes(result: MutableList<Name> = mutableListOf()): List<Name> {
        for (superTypeRef in superTypeRefs) {
            superTypeRef.collectCallableNamesFromThisAndSupertypes(result)
        }
        return result
    }

    private fun FirClass<*>.getPrimaryConstructorIfAny(): FirConstructor? =
        declarations.filterIsInstance<FirConstructor>().firstOrNull()?.takeIf { it.isPrimary }

    private fun IrClass.addFakeOverrides(klass: FirClass<*>, processedCallableNames: MutableList<Name>) {
        if (fakeOverrideMode == FakeOverrideMode.NONE) return
        val superTypesCallableNames = klass.collectCallableNamesFromSupertypes()
        val useSiteMemberScope = (klass as? FirRegularClass)?.buildUseSiteMemberScope(session, ScopeSession()) ?: return
        for (name in superTypesCallableNames) {
            if (name in processedCallableNames) continue
            processedCallableNames += name
            useSiteMemberScope.processFunctionsByName(name) { functionSymbol ->
                // TODO: think about overloaded functions. May be we should process all names.
                if (functionSymbol is FirNamedFunctionSymbol) {
                    val originalFunction = functionSymbol.fir
                    val origin = IrDeclarationOrigin.FAKE_OVERRIDE
                    if (functionSymbol.isFakeOverride) {
                        // Substitution case
                        val irFunction = declarationStorage.getIrFunction(
                            originalFunction, declarationStorage.findIrParent(originalFunction), origin = origin
                        )
                        val baseSymbol = functionSymbol.overriddenSymbol
                        declarations += irFunction.setParentByParentStack().withFunction {
                            setFunctionContent(irFunction.descriptor, originalFunction, firOverriddenSymbol = baseSymbol)
                        }
                    } else if (fakeOverrideMode != FakeOverrideMode.SUBSTITUTION) {
                        // Trivial fake override case
                        val fakeOverrideSymbol =
                            FirClassSubstitutionScope.createFakeOverrideFunction(session, originalFunction, functionSymbol)
                        val fakeOverrideFunction = fakeOverrideSymbol.fir

                        val irFunction = declarationStorage.getIrFunction(
                            fakeOverrideFunction, declarationStorage.findIrParent(originalFunction), origin = origin
                        )
                        declarations += irFunction.setParentByParentStack().withFunction {
                            setFunctionContent(irFunction.descriptor, fakeOverrideFunction, firOverriddenSymbol = functionSymbol)
                        }
                    }
                }
                ProcessorAction.STOP
            }
            useSiteMemberScope.processPropertiesByName(name) { propertySymbol ->
                if (propertySymbol is FirPropertySymbol) {
                    val originalProperty = propertySymbol.fir
                    val origin = IrDeclarationOrigin.FAKE_OVERRIDE
                    if (propertySymbol.isFakeOverride) {
                        // Substitution case
                        val irProperty = declarationStorage.getIrProperty(
                            originalProperty, declarationStorage.findIrParent(originalProperty), origin = origin
                        )
                        val baseSymbol = propertySymbol.overriddenSymbol
                        declarations += irProperty.setParentByParentStack().withProperty {
                            setPropertyContent(irProperty.descriptor, originalProperty, firOverriddenSymbol = baseSymbol)
                        }
                    } else if (fakeOverrideMode != FakeOverrideMode.SUBSTITUTION) {
                        // Trivial fake override case
                        val fakeOverrideSymbol =
                            FirClassSubstitutionScope.createFakeOverrideProperty(session, originalProperty, propertySymbol)
                        val fakeOverrideProperty = fakeOverrideSymbol.fir

                        val irProperty = declarationStorage.getIrProperty(
                            fakeOverrideProperty, declarationStorage.findIrParent(originalProperty), origin = origin
                        )
                        declarations += irProperty.setParentByParentStack().withProperty {
                            setPropertyContent(irProperty.descriptor, fakeOverrideProperty, firOverriddenSymbol = propertySymbol)
                        }
                    }
                }
                ProcessorAction.STOP
            }
        }
    }

    private fun IrClass.setClassContent(klass: FirClass<*>) {
        declarationStorage.enterScope(descriptor)
        val primaryConstructor = klass.getPrimaryConstructorIfAny()
        val irPrimaryConstructor = primaryConstructor?.accept(this@Fir2IrVisitor, null) as IrConstructor?
        withClass {
            if (irPrimaryConstructor != null) {
                declarations += irPrimaryConstructor
            }
            val processedCallableNames = mutableListOf<Name>()
            klass.declarations.forEach {
                if (it !is FirConstructor || !it.isPrimary) {
                    val irDeclaration = it.toIrDeclaration() ?: return@forEach
                    declarations += irDeclaration
                    if (it is FirMemberDeclaration && (it is FirSimpleFunction || it is FirProperty)) {
                        processedCallableNames += it.name
                    }
                }
            }
            addFakeOverrides(klass, processedCallableNames)
            klass.annotations.forEach {
                val irCall = it.accept(this@Fir2IrVisitor, null) as? IrConstructorCall ?: return@forEach
                annotations += irCall
            }
        }
        if (irPrimaryConstructor != null) {
            declarationStorage.leaveScope(irPrimaryConstructor.descriptor)
        }
        declarationStorage.leaveScope(descriptor)
    }

    override fun visitRegularClass(regularClass: FirRegularClass, data: Any?): IrElement {
        return declarationStorage.getIrClass(regularClass, setParent = false)
            .setParentByParentStack()
            .withParent {
                setClassContent(regularClass)
            }
    }

    private fun IrFunction.addDispatchReceiverParameter(containingClass: IrClass) {
        val thisOrigin = IrDeclarationOrigin.DEFINED
        val thisType = containingClass.thisReceiver!!.type
        val descriptor = WrappedValueParameterDescriptor()
        dispatchReceiverParameter = symbolTable.declareValueParameter(
            startOffset, endOffset, thisOrigin, descriptor,
            thisType
        ) { symbol ->
            IrValueParameterImpl(
                startOffset, endOffset, thisOrigin, symbol,
                Name.special("<this>"), -1, thisType,
                varargElementType = null, isCrossinline = false, isNoinline = false
            ).setParentByParentStack()
        }.also { descriptor.bind(it) }
    }

    private fun <T : IrFunction> T.setFunctionContent(
        descriptor: FunctionDescriptor,
        firFunction: FirFunction<*>?,
        firOverriddenSymbol: FirNamedFunctionSymbol? = null
    ): T {
        setParentByParentStack()
        withParent {
            val firFunctionSymbol = (firFunction as? FirSimpleFunction)?.symbol
            val lastClass = classStack.lastOrNull()
            val containingClass = if (firOverriddenSymbol == null || firFunctionSymbol == null) {
                lastClass
            } else {
                val callableId = firFunctionSymbol.callableId
                val ownerClassId = callableId.classId
                if (ownerClassId == null) {
                    lastClass
                } else {
                    val classLikeSymbol = session.firSymbolProvider.getClassLikeSymbolByFqName(ownerClassId)
                    if (classLikeSymbol !is FirClassSymbol) {
                        lastClass
                    } else {
                        val firClass = classLikeSymbol.fir as FirClass<*>
                        declarationStorage.getIrClass(firClass, setParent = false)
                    }
                }
            }
            if (firFunction !is FirConstructor && containingClass != null) {
                addDispatchReceiverParameter(containingClass)
            }
            if (firFunction != null) {
                for ((valueParameter, firValueParameter) in valueParameters.zip(firFunction.valueParameters)) {
                    valueParameter.setDefaultValue(firValueParameter)
                }
            }
            if (firOverriddenSymbol != null && this is IrSimpleFunction && firFunctionSymbol != null) {
                val overriddenSymbol = declarationStorage.getIrFunctionSymbol(firOverriddenSymbol)
                if (overriddenSymbol is IrSimpleFunctionSymbol) {
                    overriddenSymbols += overriddenSymbol
                }
            }
            var body = firFunction?.body?.convertToIrBlockBody()
            if (firFunction is FirConstructor && this is IrConstructor && !parentAsClass.isAnnotationClass) {
                if (body == null) {
                    body = IrBlockBodyImpl(startOffset, endOffset)
                }
                val delegatedConstructor = firFunction.delegatedConstructor
                if (delegatedConstructor != null) {
                    val irDelegatingConstructorCall = delegatedConstructor.toIrDelegatingConstructorCall()
                    body.statements += irDelegatingConstructorCall ?: delegatedConstructor.convertWithOffsets { startOffset, endOffset ->
                        IrErrorCallExpressionImpl(
                            startOffset, endOffset, returnType, "Cannot find delegated constructor call"
                        )
                    }
                }
                if (delegatedConstructor?.isThis == false) {
                    val irClass = parent as IrClass
                    body.statements += IrInstanceInitializerCallImpl(
                        startOffset, endOffset, irClass.symbol, constructedClassType
                    )
                }
                if (body.statements.isNotEmpty()) {
                    this.body = body
                }
            } else if (this !is IrConstructor) {
                this.body = body
            }
            if (this !is IrConstructor || !this.isPrimary) {
                // Scope for primary constructor should be left after class declaration
                declarationStorage.leaveScope(descriptor)
            }
        }
        return this
    }

    override fun visitConstructor(constructor: FirConstructor, data: Any?): IrElement {
        val irConstructor = declarationStorage.getIrConstructor(
            constructor, irParent = parentStack.last() as? IrClass
        )
        return irConstructor.setParentByParentStack().withFunction {
            setFunctionContent(irConstructor.descriptor, constructor)
        }
    }

    override fun visitAnonymousInitializer(anonymousInitializer: FirAnonymousInitializer, data: Any?): IrElement {
        val origin = IrDeclarationOrigin.DEFINED
        val parent = parentStack.last() as IrClass
        return anonymousInitializer.convertWithOffsets { startOffset, endOffset ->
            symbolTable.declareAnonymousInitializer(
                startOffset, endOffset, origin, parent.descriptor
            ).apply {
                setParentByParentStack()
                declarationStorage.enterScope(descriptor)
                body = anonymousInitializer.body!!.convertToIrBlockBody()
                declarationStorage.leaveScope(descriptor)
            }
        }
    }

    private fun FirDelegatedConstructorCall.toIrDelegatingConstructorCall(): IrDelegatingConstructorCall? {
        val constructedClassSymbol = with(typeContext) {
            (constructedTypeRef as FirResolvedTypeRef).type.typeConstructor()
        } as? FirClassSymbol<*> ?: return null
        val constructedIrType = constructedTypeRef.toIrType(this@Fir2IrVisitor.session, declarationStorage)
        // TODO: find delegated constructor correctly
        val classId = constructedClassSymbol.classId
        var constructorSymbol: FirConstructorSymbol? = null
        constructedClassSymbol.buildUseSiteMemberScope(this@Fir2IrVisitor.session, ScopeSession())!!.processFunctionsByName(
            classId.shortClassName
        ) {
            when {
                it !is FirConstructorSymbol -> ProcessorAction.NEXT
                arguments.size <= it.fir.valueParameters.size -> {
                    constructorSymbol = it
                    ProcessorAction.STOP
                }
                else -> ProcessorAction.NEXT
            }
        }
        val foundConstructorSymbol = constructorSymbol ?: return null
        return convertWithOffsets { startOffset, endOffset ->
            IrDelegatingConstructorCallImpl(
                startOffset, endOffset,
                constructedIrType,
                declarationStorage.getIrFunctionSymbol(foundConstructorSymbol) as IrConstructorSymbol
            ).apply {
                for ((index, argument) in arguments.withIndex()) {
                    val argumentExpression = argument.toIrExpression()
                    putValueArgument(index, argumentExpression)
                }
            }
        }
    }

    override fun visitSimpleFunction(simpleFunction: FirSimpleFunction, data: Any?): IrElement {
        val irFunction = declarationStorage.getIrFunction(
            simpleFunction, irParent = parentStack.last() as? IrClass
        )
        return irFunction.setParentByParentStack().withFunction {
            setFunctionContent(irFunction.descriptor, simpleFunction)
        }
    }

    override fun visitAnonymousFunction(anonymousFunction: FirAnonymousFunction, data: Any?): IrElement =
        anonymousFunction.convertWithOffsets { startOffset, endOffset ->
            val irFunction = declarationStorage.getIrLocalFunction(anonymousFunction)
            irFunction.setParentByParentStack().withFunction {
                setFunctionContent(irFunction.descriptor, anonymousFunction)
            }

            val type = anonymousFunction.typeRef.toIrType(session, declarationStorage)

            IrFunctionExpressionImpl(startOffset, endOffset, type, irFunction, IrStatementOrigin.LAMBDA)
        }

    private fun IrValueParameter.setDefaultValue(firValueParameter: FirValueParameter) {
        val firDefaultValue = firValueParameter.defaultValue
        if (firDefaultValue != null) {
            this.defaultValue = IrExpressionBodyImpl(firDefaultValue.toIrExpression())
        }
    }

    private fun visitLocalVariable(variable: FirProperty): IrElement {
        assert(variable.isLocal)
        val irVariable = declarationStorage.createAndSaveIrVariable(variable)
        return irVariable.setParentByParentStack().apply {
            val initializer = variable.initializer
            if (initializer != null) {
                this.initializer = initializer.toIrExpression()
            }
        }
    }

    private fun IrProperty.createBackingField(
        property: FirProperty,
        origin: IrDeclarationOrigin,
        descriptor: PropertyDescriptor,
        visibility: Visibility,
        name: Name,
        isFinal: Boolean,
        firInitializerExpression: FirExpression?,
        type: IrType? = null
    ): IrField {
        val inferredType = type ?: firInitializerExpression!!.typeRef.toIrType(session, declarationStorage)
        return symbolTable.declareField(
            startOffset, endOffset, origin, descriptor, inferredType
        ) { symbol ->
            IrFieldImpl(
                startOffset, endOffset, origin, symbol,
                name, inferredType,
                visibility, isFinal = isFinal, isExternal = false,
                isStatic = property.isStatic || parent !is IrClass,
                isFakeOverride = origin == IrDeclarationOrigin.FAKE_OVERRIDE
            )
        }.setParentByParentStack().withParent {
            declarationStorage.enterScope(descriptor)
            val initializerExpression = firInitializerExpression?.toIrExpression()
            initializer = initializerExpression?.let { IrExpressionBodyImpl(it) }
            correspondingPropertySymbol = this@createBackingField.symbol
            declarationStorage.leaveScope(descriptor)
        }
    }

    private fun IrProperty.setPropertyContent(
        descriptor: PropertyDescriptor,
        property: FirProperty,
        firOverriddenSymbol: FirPropertySymbol? = null
    ): IrProperty {
        declarationStorage.enterScope(descriptor)
        val initializer = property.initializer
        val delegate = property.delegate
        val irParent = this.parent
        val propertyType = property.returnTypeRef.toIrType(session, declarationStorage)
        // TODO: this checks are very preliminary, FIR resolve should determine backing field presence itself
        // TODO (2): backing field should be created inside declaration storage
        if (property.modality != Modality.ABSTRACT && (irParent !is IrClass || !irParent.isInterface)) {
            if (initializer != null || property.getter is FirDefaultPropertyGetter ||
                property.isVar && property.setter is FirDefaultPropertySetter
            ) {
                backingField = createBackingField(
                    property, IrDeclarationOrigin.PROPERTY_BACKING_FIELD, descriptor,
                    Visibilities.PRIVATE, property.name, property.isVal, initializer, propertyType
                )
            } else if (delegate != null) {
                backingField = createBackingField(
                    property, IrDeclarationOrigin.DELEGATE, descriptor,
                    Visibilities.PRIVATE, Name.identifier("${property.name}\$delegate"), true, delegate
                )
            }
            val backingField = backingField
            if (firOverriddenSymbol != null && backingField != null) {
                val overriddenSymbol = declarationStorage.getIrPropertyOrFieldSymbol(firOverriddenSymbol.fir.backingFieldSymbol)
                if (overriddenSymbol is IrFieldSymbol) {
                    backingField.overriddenSymbols += overriddenSymbol
                }
            }
        }
        val overriddenProperty = firOverriddenSymbol?.let { declarationStorage.getIrPropertyOrFieldSymbol(it) } as? IrPropertySymbol
        getter?.setPropertyAccessorContent(
            property.getter, this, propertyType, property.getter is FirDefaultPropertyGetter, property.getter == null
        )
        getter?.apply {
            overriddenProperty?.owner?.getter?.symbol?.let { overriddenSymbols += it }
        }
        if (property.isVar) {
            setter?.setPropertyAccessorContent(
                property.setter, this, propertyType, property.setter is FirDefaultPropertySetter, property.setter == null
            )
            setter?.apply {
                overriddenProperty?.owner?.setter?.symbol?.let { overriddenSymbols += it }
            }
        }
        property.annotations.forEach {
            annotations += it.accept(this@Fir2IrVisitor, null) as IrConstructorCall
        }
        declarationStorage.leaveScope(descriptor)
        return this
    }

    override fun visitProperty(property: FirProperty, data: Any?): IrElement {
        if (property.isLocal) return visitLocalVariable(property)
        val irProperty = declarationStorage.getIrProperty(property, irParent = parentStack.last() as? IrClass)
        return irProperty.setParentByParentStack().withProperty { setPropertyContent(irProperty.descriptor, property) }
    }

    private fun IrFieldAccessExpression.setReceiver(declaration: IrDeclaration): IrFieldAccessExpression {
        if (declaration is IrFunction) {
            val dispatchReceiver = declaration.dispatchReceiverParameter
            if (dispatchReceiver != null) {
                receiver = IrGetValueImpl(startOffset, endOffset, dispatchReceiver.symbol)
            }
        }
        return this
    }

    private fun IrFunction.setPropertyAccessorContent(
        propertyAccessor: FirPropertyAccessor?,
        correspondingProperty: IrProperty,
        propertyType: IrType,
        isDefault: Boolean,
        isFakeOverride: Boolean
    ) {
        withFunction {
            if (propertyAccessor != null) {
                with(declarationStorage) { this@setPropertyAccessorContent.enterLocalScope(propertyAccessor) }
            } else {
                declarationStorage.enterScope(descriptor)
            }
            setFunctionContent(descriptor, propertyAccessor)
            if (isDefault || isFakeOverride) {
                withParent {
                    declarationStorage.enterScope(descriptor)
                    val backingField = correspondingProperty.backingField
                    val fieldSymbol = symbolTable.referenceField(correspondingProperty.descriptor)
                    val declaration = this
                    if (!isFakeOverride && backingField != null) {
                        body = IrBlockBodyImpl(
                            startOffset, endOffset,
                            listOf(
                                if (isSetter) {
                                    IrSetFieldImpl(startOffset, endOffset, fieldSymbol, unitType).apply {
                                        setReceiver(declaration)
                                        value = IrGetValueImpl(startOffset, endOffset, propertyType, valueParameters.first().symbol)
                                    }
                                } else {
                                    IrReturnImpl(
                                        startOffset, endOffset, nothingType, symbol,
                                        IrGetFieldImpl(startOffset, endOffset, fieldSymbol, propertyType).setReceiver(declaration)
                                    )
                                }
                            )
                        )
                    }
                    declarationStorage.leaveScope(descriptor)
                }
            }
        }
    }


    override fun visitReturnExpression(returnExpression: FirReturnExpression, data: Any?): IrElement {
        val firTarget = returnExpression.target.labeledElement
        var irTarget = functionStack.last()
        for (potentialTarget in functionStack.asReversed()) {
            // TODO: remove comparison by name
            if (potentialTarget.name == (firTarget as? FirSimpleFunction)?.name) {
                irTarget = potentialTarget
                break
            }
        }
        return returnExpression.convertWithOffsets { startOffset, endOffset ->
            val result = returnExpression.result
            val descriptor = irTarget.descriptor
            IrReturnImpl(
                startOffset, endOffset, nothingType,
                when (descriptor) {
                    is ClassConstructorDescriptor -> symbolTable.referenceConstructor(descriptor)
                    else -> symbolTable.referenceSimpleFunction(descriptor)
                },
                result.toIrExpression()
            )
        }
    }

    override fun visitWrappedArgumentExpression(wrappedArgumentExpression: FirWrappedArgumentExpression, data: Any?): IrElement {
        // TODO: change this temporary hack to something correct
        return wrappedArgumentExpression.expression.toIrExpression()
    }

    private fun FirReference.statementOrigin(): IrStatementOrigin? {
        return when (this) {
            is FirPropertyFromParameterResolvedNamedReference -> IrStatementOrigin.INITIALIZE_PROPERTY_FROM_PARAMETER
            is FirResolvedNamedReference -> when (resolvedSymbol) {
                is AccessorSymbol, is SyntheticPropertySymbol -> IrStatementOrigin.GET_PROPERTY
                else -> null
            }
            else -> null
        }
    }

    private fun FirQualifiedAccess.toIrExpression(typeRef: FirTypeRef): IrExpression {
        val type = typeRef.toIrType(this@Fir2IrVisitor.session, declarationStorage)
        val symbol = calleeReference.toSymbol(declarationStorage)
        return typeRef.convertWithOffsets { startOffset, endOffset ->
            when {
                symbol is IrConstructorSymbol -> IrConstructorCallImpl.fromSymbolOwner(startOffset, endOffset, type, symbol)
                symbol is IrSimpleFunctionSymbol -> IrCallImpl(
                    startOffset, endOffset, type, symbol, origin = calleeReference.statementOrigin()
                )
                symbol is IrPropertySymbol && symbol.isBound -> {
                    val getter = symbol.owner.getter
                    if (getter != null) {
                        IrCallImpl(startOffset, endOffset, type, getter.symbol)
                    } else {
                        IrErrorCallExpressionImpl(startOffset, endOffset, type, "No getter found for ${calleeReference.render()}")
                    }
                }
                symbol is IrFieldSymbol -> IrGetFieldImpl(startOffset, endOffset, symbol, type, origin = IrStatementOrigin.GET_PROPERTY)
                symbol is IrValueSymbol -> IrGetValueImpl(
                    startOffset, endOffset, type, symbol,
                    origin = calleeReference.statementOrigin()
                )
                symbol is IrEnumEntrySymbol -> IrGetEnumValueImpl(startOffset, endOffset, type, symbol)
                else -> generateErrorCallExpression(startOffset, endOffset, calleeReference, type)
            }
        }
    }

    private fun FirAnnotationCall.toIrExpression(): IrExpression {
        val coneType = (annotationTypeRef as? FirResolvedTypeRef)?.type as? ConeLookupTagBasedType
        val firSymbol = coneType?.lookupTag?.toSymbol(session) as? FirClassSymbol
        val type = coneType?.toIrType(session, declarationStorage, irBuiltIns)
        val symbol = type?.classifierOrNull
        return convertWithOffsets { startOffset, endOffset ->
            when (symbol) {
                is IrClassSymbol -> {
                    val irClass = symbol.owner
                    val fir = firSymbol?.fir as? FirClass<*>
                    val irConstructor = fir?.getPrimaryConstructorIfAny()?.let { firConstructor ->
                        declarationStorage.getIrConstructor(firConstructor, irParent = irClass, shouldLeaveScope = true)
                    }?.apply {
                        this.parent = irClass
                    }
                    if (irConstructor == null) {
                        IrErrorCallExpressionImpl(startOffset, endOffset, type, "No annotation constructor found: ${irClass.name}")
                    } else {
                        IrConstructorCallImpl.fromSymbolDescriptor(startOffset, endOffset, type, irConstructor.symbol)
                    }

                }
                else -> {
                    IrErrorCallExpressionImpl(startOffset, endOffset, type ?: createErrorType(), "Unresolved reference: ${render()}")
                }
            }
        }
    }

    private fun IrExpression.applyCallArguments(call: FirCall): IrExpression {
        return when (this) {
            is IrCallWithIndexedArgumentsBase -> {
                val argumentsCount = call.arguments.size
                if (argumentsCount <= valueArgumentsCount) {
                    apply {
                        for ((index, argument) in call.arguments.withIndex()) {
                            val argumentExpression = argument.toIrExpression()
                            putValueArgument(index, argumentExpression)
                        }
                    }
                } else {
                    val name = if (this is IrCallImpl) symbol.owner.name else "???"
                    IrErrorCallExpressionImpl(
                        startOffset, endOffset, type,
                        "Cannot bind $argumentsCount arguments to $name call with $valueArgumentsCount parameters"
                    ).apply {
                        for (argument in call.arguments) {
                            addArgument(argument.toIrExpression())
                        }
                    }
                }
            }
            is IrErrorCallExpressionImpl -> apply {
                for (argument in call.arguments) {
                    addArgument(argument.toIrExpression())
                }
            }
            else -> this
        }
    }

    private fun IrExpression.applyTypeArguments(call: FirFunctionCall): IrExpression {
        return when (this) {
            is IrCallWithIndexedArgumentsBase -> {
                val argumentsCount = call.typeArguments.size
                if (argumentsCount <= typeArgumentsCount) {
                    apply {
                        for ((index, argument) in call.typeArguments.withIndex()) {
                            val argumentIrType = (argument as FirTypeProjectionWithVariance).typeRef.toIrType(
                                session,
                                declarationStorage
                            )
                            putTypeArgument(index, argumentIrType)
                        }
                    }
                } else {
                    val name = if (this is IrCallImpl) symbol.owner.name else "???"
                    IrErrorExpressionImpl(
                        startOffset, endOffset, type,
                        "Cannot bind $argumentsCount type arguments to $name call with $typeArgumentsCount type parameters"
                    )
                }
            }
            else -> this
        }
    }

    private fun FirQualifiedAccess.findIrDispatchReceiver(): IrExpression? = findIrReceiver(isDispatch = true)

    private fun FirQualifiedAccess.findIrExtensionReceiver(): IrExpression? = findIrReceiver(isDispatch = false)

    private fun FirQualifiedAccess.findIrReceiver(isDispatch: Boolean): IrExpression? {
        val firReceiver = if (isDispatch) dispatchReceiver else extensionReceiver
        return firReceiver.takeIf { it !is FirNoReceiverExpression }?.toIrExpression()
            ?: explicitReceiver?.toIrExpression() // NB: this applies to the situation when call is unresolved
            ?: run {
                // Object case
                val callableReference = calleeReference as? FirResolvedNamedReference
                val ownerClassId = (callableReference?.resolvedSymbol as? FirCallableSymbol<*>)?.callableId?.classId
                val ownerClassSymbol = ownerClassId?.let { session.firSymbolProvider.getClassLikeSymbolByFqName(it) }
                val firClass = (ownerClassSymbol?.fir as? FirClass)?.takeIf {
                    it is FirAnonymousObject || it is FirRegularClass && it.classKind == ClassKind.OBJECT
                }
                firClass?.convertWithOffsets { startOffset, endOffset ->
                    val irClass = declarationStorage.getIrClass(firClass, setParent = false)
                    IrGetObjectValueImpl(startOffset, endOffset, irClass.defaultType, irClass.symbol)
                }
            }
        // TODO: uncomment after fixing KT-35730
//            ?: run {
//                val name = if (isDispatch) "Dispatch" else "Extension"
//                throw AssertionError(
//                    "$name receiver expected: ${render()} to ${calleeReference.render()}"
//                )
//            }
    }

    private fun IrExpression.applyReceivers(qualifiedAccess: FirQualifiedAccess): IrExpression {
        return when (this) {
            is IrCallImpl -> {
                val ownerFunction = symbol.owner
                if (ownerFunction.dispatchReceiverParameter != null) {
                    dispatchReceiver = qualifiedAccess.findIrDispatchReceiver()
                } else if (ownerFunction.extensionReceiverParameter != null) {
                    extensionReceiver = qualifiedAccess.findIrExtensionReceiver()
                }
                this
            }
            is IrFieldExpressionBase -> {
                val ownerField = symbol.owner
                if (!ownerField.isStatic) {
                    receiver = qualifiedAccess.findIrDispatchReceiver()
                }
                this
            }
            else -> this
        }
    }

    override fun visitFunctionCall(functionCall: FirFunctionCall, data: Any?): IrElement {
        val convertibleCall = if (functionCall.toResolvedCallableSymbol()?.fir is FirIntegerOperator) {
            functionCall.copy().transformSingle(integerApproximator, null)
        } else {
            functionCall
        }
        return convertibleCall.toIrExpression(convertibleCall.typeRef)
            .applyCallArguments(convertibleCall).applyTypeArguments(convertibleCall).applyReceivers(convertibleCall)
    }

    override fun visitAnnotationCall(annotationCall: FirAnnotationCall, data: Any?): IrElement {
        return annotationCall.toIrExpression().applyCallArguments(annotationCall)
    }

    override fun visitQualifiedAccessExpression(qualifiedAccessExpression: FirQualifiedAccessExpression, data: Any?): IrElement {
        return qualifiedAccessExpression.toIrExpression(qualifiedAccessExpression.typeRef).applyReceivers(qualifiedAccessExpression)
    }

    override fun visitThisReceiverExpression(thisReceiverExpression: FirThisReceiverExpression, data: Any?): IrElement {
        return visitQualifiedAccessExpression(thisReceiverExpression, data)
    }

    override fun visitExpressionWithSmartcast(expressionWithSmartcast: FirExpressionWithSmartcast, data: Any?): IrElement {
        return visitQualifiedAccessExpression(expressionWithSmartcast, data)
    }

    override fun visitCallableReferenceAccess(callableReferenceAccess: FirCallableReferenceAccess, data: Any?): IrElement {
        val symbol = callableReferenceAccess.calleeReference.toSymbol(declarationStorage)
        val type = callableReferenceAccess.typeRef.toIrType(this@Fir2IrVisitor.session, declarationStorage)
        return callableReferenceAccess.convertWithOffsets { startOffset, endOffset ->
            when (symbol) {
                is IrPropertySymbol -> {
                    IrPropertyReferenceImpl(
                        startOffset, endOffset, type, symbol, 0,
                        symbol.owner.backingField?.symbol,
                        symbol.owner.getter?.symbol,
                        symbol.owner.setter?.symbol
                    )
                }
                is IrFunctionSymbol -> {
                    IrFunctionReferenceImpl(
                        startOffset, endOffset, type, symbol,
                        0
                    )
                }
                else -> {
                    IrErrorCallExpressionImpl(
                        startOffset, endOffset, type, "Unsupported callable reference: ${callableReferenceAccess.render()}"
                    )
                }
            }
        }
    }

    private fun generateErrorCallExpression(
        startOffset: Int,
        endOffset: Int,
        calleeReference: FirReference,
        type: IrType? = null
    ): IrErrorCallExpression {
        return IrErrorCallExpressionImpl(
            startOffset, endOffset, type ?: createErrorType(),
            "Unresolved reference: ${calleeReference.render()}"
        )
    }

    override fun visitVariableAssignment(variableAssignment: FirVariableAssignment, data: Any?): IrElement {
        val calleeReference = variableAssignment.calleeReference
        val symbol = calleeReference.toSymbol(declarationStorage)
        return variableAssignment.convertWithOffsets { startOffset, endOffset ->
            if (symbol != null && symbol.isBound) {
                when (symbol) {
                    is IrFieldSymbol -> IrSetFieldImpl(
                        startOffset, endOffset, symbol, unitType
                    ).apply {
                        value = variableAssignment.rValue.toIrExpression()
                    }
                    is IrPropertySymbol -> {
                        val irProperty = symbol.owner
                        val backingField = irProperty.backingField
                        if (backingField != null) {
                            IrSetFieldImpl(
                                startOffset, endOffset, backingField.symbol, unitType
                            ).apply {
                                value = variableAssignment.rValue.toIrExpression()
                            }
                        } else {
                            generateErrorCallExpression(startOffset, endOffset, calleeReference)
                        }
                    }
                    is IrVariableSymbol -> {
                        IrSetVariableImpl(
                            startOffset, endOffset, symbol.owner.type, symbol, variableAssignment.rValue.toIrExpression(), null
                        )
                    }
                    else -> generateErrorCallExpression(startOffset, endOffset, calleeReference)
                }
            } else {
                generateErrorCallExpression(startOffset, endOffset, calleeReference)
            }
        }.applyReceivers(variableAssignment)
    }

    override fun <T> visitConstExpression(constExpression: FirConstExpression<T>, data: Any?): IrElement {
        return constExpression.convertWithOffsets { startOffset, endOffset ->
            @Suppress("UNCHECKED_CAST")
            val kind = constExpression.getIrConstKind() as IrConstKind<T>

            @Suppress("UNCHECKED_CAST")
            val value = (constExpression.value as? Long)?.let {
                when (kind) {
                    IrConstKind.Byte -> it.toByte()
                    IrConstKind.Short -> it.toShort()
                    IrConstKind.Int -> it.toInt()
                    IrConstKind.Float -> it.toFloat()
                    IrConstKind.Double -> it.toDouble()
                    else -> it
                }
            } as T ?: constExpression.value
            IrConstImpl(
                startOffset, endOffset,
                constExpression.typeRef.toIrType(session, declarationStorage),
                kind, value
            )
        }
    }

    override fun visitAnonymousObject(anonymousObject: FirAnonymousObject, data: Any?): IrElement {
        val anonymousClass = declarationStorage.getIrAnonymousObject(anonymousObject).setParentByParentStack().withParent {
            setClassContent(anonymousObject)
        }
        val anonymousClassType = anonymousClass.thisReceiver!!.type
        return anonymousObject.convertWithOffsets { startOffset, endOffset ->
            IrBlockImpl(
                startOffset, endOffset, anonymousClassType, IrStatementOrigin.OBJECT_LITERAL,
                listOf(
                    anonymousClass,
                    IrConstructorCallImpl.fromSymbolOwner(
                        startOffset,
                        endOffset,
                        anonymousClassType,
                        anonymousClass.constructors.first().symbol,
                        anonymousClass.typeParameters.size
                    )
                )
            )
        }
    }

    // ==================================================================================

    private fun FirStatement.toIrStatement(): IrStatement? {
        if (this is FirTypeAlias) return null
        if (this is FirUnitExpression) return toIrExpression()
        return accept(this@Fir2IrVisitor, null) as IrStatement
    }

    private fun FirExpression.toIrExpression(): IrExpression {
        return when (this) {
            is FirBlock -> convertToIrExpressionOrBlock()
            is FirUnitExpression -> convertWithOffsets { startOffset, endOffset ->
                IrGetObjectValueImpl(
                    startOffset, endOffset, unitType,
                    symbolTable.referenceClass(irBuiltIns.builtIns.unit)
                )
            }
            else -> accept(this@Fir2IrVisitor, null) as IrExpression
        }
    }

    private fun FirBlock.convertToIrBlockBody(): IrBlockBody {
        return convertWithOffsets { startOffset, endOffset ->
            val irStatements = statements.map { it.toIrStatement() }
            IrBlockBodyImpl(
                startOffset, endOffset,
                if (irStatements.isNotEmpty()) {
                    irStatements.filterNotNull().takeIf { it.isNotEmpty() }
                        ?: listOf(IrBlockImpl(startOffset, endOffset, unitType, null, emptyList()))
                } else {
                    emptyList()
                }
            )
        }
    }

    private fun FirBlock.convertToIrExpressionOrBlock(): IrExpression {
        if (statements.size == 1) {
            val firStatement = statements.single()
            if (firStatement is FirExpression) {
                return firStatement.toIrExpression()
            }
        }
        val type =
            (statements.lastOrNull() as? FirExpression)?.typeRef?.toIrType(this@Fir2IrVisitor.session, declarationStorage) ?: unitType
        return convertWithOffsets { startOffset, endOffset ->
            IrBlockImpl(
                startOffset, endOffset, type, null,
                statements.mapNotNull { it.toIrStatement() }
            )
        }
    }

    override fun visitErrorExpression(errorExpression: FirErrorExpression, data: Any?): IrElement {
        return errorExpression.convertWithOffsets { startOffset, endOffset ->
            IrErrorExpressionImpl(
                startOffset, endOffset,
                errorExpression.typeRef.toIrType(session, declarationStorage),
                errorExpression.diagnostic.reason
            )
        }
    }

    private fun generateWhenSubjectVariable(whenExpression: FirWhenExpression): IrVariable? {
        val subjectVariable = whenExpression.subjectVariable
        val subjectExpression = whenExpression.subject
        return when {
            subjectVariable != null -> subjectVariable.accept(this, null) as IrVariable
            subjectExpression != null -> {
                val irSubject = declarationStorage.declareTemporaryVariable(subjectExpression.toIrExpression(), "subject")
                irSubject.setParentByParentStack()
            }
            else -> null
        }
    }

    override fun visitWhenExpression(whenExpression: FirWhenExpression, data: Any?): IrElement {
        val subjectVariable = generateWhenSubjectVariable(whenExpression)
        val origin = when (val psi = whenExpression.psi) {
            is KtWhenExpression -> IrStatementOrigin.WHEN
            is KtIfExpression -> IrStatementOrigin.IF
            is KtBinaryExpression -> when (psi.operationToken) {
                KtTokens.ELVIS -> IrStatementOrigin.ELVIS
                KtTokens.OROR -> IrStatementOrigin.OROR
                KtTokens.ANDAND -> IrStatementOrigin.ANDAND
                else -> null
            }
            is KtUnaryExpression -> IrStatementOrigin.EXCLEXCL
            else -> null
        }
        return subjectVariable.withSubject {
            whenExpression.convertWithOffsets { startOffset, endOffset ->
                val irWhen = IrWhenImpl(
                    startOffset, endOffset,
                    whenExpression.typeRef.toIrType(session, declarationStorage),
                    origin
                ).apply {
                    for (branch in whenExpression.branches) {
                        if (branch.condition !is FirElseIfTrueCondition || branch.result.statements.isNotEmpty()) {
                            branches += branch.accept(this@Fir2IrVisitor, data) as IrBranch
                        }
                    }
                }
                if (subjectVariable == null) {
                    irWhen
                } else {
                    IrBlockImpl(startOffset, endOffset, irWhen.type, origin, listOf(subjectVariable, irWhen))
                }
            }
        }
    }

    override fun visitWhenBranch(whenBranch: FirWhenBranch, data: Any?): IrElement {
        return whenBranch.convertWithOffsets { startOffset, endOffset ->
            val condition = whenBranch.condition
            val irResult = whenBranch.result.toIrExpression()
            if (condition is FirElseIfTrueCondition) {
                IrElseBranchImpl(IrConstImpl.boolean(irResult.startOffset, irResult.endOffset, booleanType, true), irResult)
            } else {
                IrBranchImpl(startOffset, endOffset, condition.toIrExpression(), irResult)
            }
        }
    }

    override fun visitWhenSubjectExpression(whenSubjectExpression: FirWhenSubjectExpression, data: Any?): IrElement {
        val lastSubjectVariable = subjectVariableStack.last()
        return whenSubjectExpression.convertWithOffsets { startOffset, endOffset ->
            IrGetValueImpl(startOffset, endOffset, lastSubjectVariable.type, lastSubjectVariable.symbol)
        }
    }

    private val loopMap = mutableMapOf<FirLoop, IrLoop>()

    override fun visitDoWhileLoop(doWhileLoop: FirDoWhileLoop, data: Any?): IrElement {
        return doWhileLoop.convertWithOffsets { startOffset, endOffset ->
            IrDoWhileLoopImpl(
                startOffset, endOffset, unitType,
                IrStatementOrigin.DO_WHILE_LOOP
            ).apply {
                loopMap[doWhileLoop] = this
                label = doWhileLoop.label?.name
                body = doWhileLoop.block.convertToIrExpressionOrBlock()
                condition = doWhileLoop.condition.toIrExpression()
                loopMap.remove(doWhileLoop)
            }
        }
    }

    override fun visitWhileLoop(whileLoop: FirWhileLoop, data: Any?): IrElement {
        return whileLoop.convertWithOffsets { startOffset, endOffset ->
            IrWhileLoopImpl(
                startOffset, endOffset, unitType,
                if (whileLoop.psi is KtForExpression) IrStatementOrigin.FOR_LOOP_INNER_WHILE
                else IrStatementOrigin.WHILE_LOOP
            ).apply {
                loopMap[whileLoop] = this
                label = whileLoop.label?.name
                condition = whileLoop.condition.toIrExpression()
                body = whileLoop.block.convertToIrExpressionOrBlock()
                loopMap.remove(whileLoop)
            }
        }
    }

    private fun FirJump<FirLoop>.convertJumpWithOffsets(
        f: (startOffset: Int, endOffset: Int, irLoop: IrLoop) -> IrBreakContinueBase
    ): IrExpression {
        return convertWithOffsets { startOffset, endOffset ->
            val firLoop = target.labeledElement
            val irLoop = loopMap[firLoop]
            if (irLoop == null) {
                IrErrorExpressionImpl(startOffset, endOffset, nothingType, "Unbound loop: ${render()}")
            } else {
                f(startOffset, endOffset, irLoop).apply {
                    label = irLoop.label.takeIf { target.labelName != null }
                }
            }
        }
    }

    override fun visitBreakExpression(breakExpression: FirBreakExpression, data: Any?): IrElement {
        return breakExpression.convertJumpWithOffsets { startOffset, endOffset, irLoop ->
            IrBreakImpl(startOffset, endOffset, nothingType, irLoop)
        }
    }

    override fun visitContinueExpression(continueExpression: FirContinueExpression, data: Any?): IrElement {
        return continueExpression.convertJumpWithOffsets { startOffset, endOffset, irLoop ->
            IrContinueImpl(startOffset, endOffset, nothingType, irLoop)
        }
    }

    override fun visitThrowExpression(throwExpression: FirThrowExpression, data: Any?): IrElement {
        return throwExpression.convertWithOffsets { startOffset, endOffset ->
            IrThrowImpl(startOffset, endOffset, nothingType, throwExpression.exception.toIrExpression())
        }
    }

    override fun visitTryExpression(tryExpression: FirTryExpression, data: Any?): IrElement {
        return tryExpression.convertWithOffsets { startOffset, endOffset ->
            IrTryImpl(
                startOffset, endOffset, tryExpression.typeRef.toIrType(session, declarationStorage),
                tryExpression.tryBlock.convertToIrExpressionOrBlock(),
                tryExpression.catches.map { it.accept(this, data) as IrCatch },
                tryExpression.finallyBlock?.convertToIrExpressionOrBlock()
            )
        }
    }

    override fun visitCatch(catch: FirCatch, data: Any?): IrElement {
        return catch.convertWithOffsets { startOffset, endOffset ->
            val catchParameter = declarationStorage.createAndSaveIrVariable(catch.parameter)
            IrCatchImpl(startOffset, endOffset, catchParameter.setParentByParentStack()).apply {
                result = catch.block.convertToIrExpressionOrBlock()
            }
        }
    }

    private fun generateComparisonCall(
        startOffset: Int, endOffset: Int, operation: FirOperation, first: FirExpression, second: FirExpression
    ): IrExpression {
        val firstType = first.typeRef as? FirResolvedTypeRef
        val secondType = second.typeRef as? FirResolvedTypeRef
        if (firstType == null || secondType == null) {
            return IrErrorCallExpressionImpl(startOffset, endOffset, booleanType, "Comparison of arguments with unresolved types")
        }
        if (!AbstractStrictEqualityTypeChecker.strictEqualTypes(typeContext, firstType.type, secondType.type)) {
            return IrErrorCallExpressionImpl(
                startOffset, endOffset, booleanType,
                "Comparison of arguments with different types: ${firstType.type.render()}, ${secondType.type.render()}"
            )
        }
        // TODO: it's temporary hack which should be refactored
        val simpleType = when (val classId = (firstType.type as? ConeClassLikeType)?.lookupTag?.classId) {
            ClassId(FqName("kotlin"), FqName("Long"), false) -> irBuiltIns.longType
            ClassId(FqName("kotlin"), FqName("Int"), false) -> irBuiltIns.intType
            ClassId(FqName("kotlin"), FqName("Float"), false) -> irBuiltIns.floatType
            ClassId(FqName("kotlin"), FqName("Double"), false) -> irBuiltIns.doubleType
            else -> {
                return IrErrorCallExpressionImpl(
                    startOffset, endOffset, booleanType, "Comparison of arguments with unsupported type: $classId"
                )
            }
        }
        val classifier = simpleType.classifierOrFail
        val (symbol, origin) = when (operation) {
            FirOperation.LT -> irBuiltIns.lessFunByOperandType[classifier] to IrStatementOrigin.LT
            FirOperation.GT -> irBuiltIns.greaterFunByOperandType[classifier] to IrStatementOrigin.GT
            FirOperation.LT_EQ -> irBuiltIns.lessOrEqualFunByOperandType[classifier] to IrStatementOrigin.LTEQ
            FirOperation.GT_EQ -> irBuiltIns.greaterOrEqualFunByOperandType[classifier] to IrStatementOrigin.GTEQ
            else -> throw AssertionError("Unexpected comparison operation: $operation")
        }
        return primitiveOp2(startOffset, endOffset, symbol!!, booleanType, origin, first.toIrExpression(), second.toIrExpression())
    }

    private fun generateOperatorCall(
        startOffset: Int, endOffset: Int, operation: FirOperation, arguments: List<FirExpression>
    ): IrExpression {
        if (operation in FirOperation.COMPARISONS) {
            return generateComparisonCall(startOffset, endOffset, operation, arguments[0], arguments[1])
        }
        val (type, symbol, origin) = when (operation) {
            FirOperation.EQ -> Triple(booleanType, irBuiltIns.eqeqSymbol, IrStatementOrigin.EQEQ)
            FirOperation.NOT_EQ -> Triple(booleanType, irBuiltIns.eqeqSymbol, IrStatementOrigin.EXCLEQ)
            FirOperation.IDENTITY -> Triple(booleanType, irBuiltIns.eqeqeqSymbol, IrStatementOrigin.EQEQEQ)
            FirOperation.NOT_IDENTITY -> Triple(booleanType, irBuiltIns.eqeqeqSymbol, IrStatementOrigin.EXCLEQEQ)
            FirOperation.EXCL -> Triple(booleanType, irBuiltIns.booleanNotSymbol, IrStatementOrigin.EXCL)
            FirOperation.LT, FirOperation.GT,
            FirOperation.LT_EQ, FirOperation.GT_EQ,
            FirOperation.OTHER, FirOperation.ASSIGN, FirOperation.PLUS_ASSIGN,
            FirOperation.MINUS_ASSIGN, FirOperation.TIMES_ASSIGN,
            FirOperation.DIV_ASSIGN, FirOperation.REM_ASSIGN,
            FirOperation.IS, FirOperation.NOT_IS,
            FirOperation.AS, FirOperation.SAFE_AS -> {
                TODO("Should not be here: incompatible operation in FirOperatorCall: $operation")
            }
        }
        val result = if (operation in UNARY_OPERATIONS) {
            primitiveOp1(startOffset, endOffset, symbol, type, origin, arguments[0].toIrExpression())
        } else {
            primitiveOp2(startOffset, endOffset, symbol, type, origin, arguments[0].toIrExpression(), arguments[1].toIrExpression())
        }
        if (operation !in NEGATED_OPERATIONS) return result
        return primitiveOp1(startOffset, endOffset, irBuiltIns.booleanNotSymbol, booleanType, origin, result)
    }

    override fun visitOperatorCall(operatorCall: FirOperatorCall, data: Any?): IrElement {
        return operatorCall.convertWithOffsets { startOffset, endOffset ->
            generateOperatorCall(startOffset, endOffset, operatorCall.operation, operatorCall.arguments)
        }
    }

    override fun visitStringConcatenationCall(stringConcatenationCall: FirStringConcatenationCall, data: Any?): IrElement {
        return stringConcatenationCall.convertWithOffsets { startOffset, endOffset ->
            IrStringConcatenationImpl(
                startOffset, endOffset, stringType,
                stringConcatenationCall.arguments.map { it.toIrExpression() }
            )
        }
    }

    override fun visitTypeOperatorCall(typeOperatorCall: FirTypeOperatorCall, data: Any?): IrElement {
        return typeOperatorCall.convertWithOffsets { startOffset, endOffset ->
            val irTypeOperand = typeOperatorCall.conversionTypeRef.toIrType(session, declarationStorage)
            val (irType, irTypeOperator) = when (typeOperatorCall.operation) {
                FirOperation.IS -> booleanType to IrTypeOperator.INSTANCEOF
                FirOperation.NOT_IS -> booleanType to IrTypeOperator.NOT_INSTANCEOF
                FirOperation.AS -> irTypeOperand to IrTypeOperator.CAST
                FirOperation.SAFE_AS -> irTypeOperand.makeNullable() to IrTypeOperator.SAFE_CAST
                else -> TODO("Should not be here: ${typeOperatorCall.operation} in type operator call")
            }

            IrTypeOperatorCallImpl(
                startOffset, endOffset, irType, irTypeOperator, irTypeOperand,
                typeOperatorCall.argument.toIrExpression()
            )
        }
    }

    override fun visitCheckNotNullCall(checkNotNullCall: FirCheckNotNullCall, data: Any?): IrElement {
        return checkNotNullCall.convertWithOffsets { startOffset, endOffset ->
            IrCallImpl(
                startOffset, endOffset,
                checkNotNullCall.typeRef.toIrType(session, declarationStorage),
                irBuiltIns.checkNotNullSymbol,
                IrStatementOrigin.EXCLEXCL
            ).apply {
                putTypeArgument(0, checkNotNullCall.argument.typeRef.toIrType(session, declarationStorage).makeNotNull())
                putValueArgument(0, checkNotNullCall.argument.toIrExpression())
            }
        }
    }

    override fun visitGetClassCall(getClassCall: FirGetClassCall, data: Any?): IrElement {
        return getClassCall.convertWithOffsets { startOffset, endOffset ->
            val argument = getClassCall.argument
            val irType = getClassCall.typeRef.toIrType(session, declarationStorage)
            if (argument is FirResolvedReifiedParameterReference) {
                IrClassReferenceImpl(
                    startOffset, endOffset, irType,
                    argument.symbol.toTypeParameterSymbol(declarationStorage),
                    argument.typeRef.toIrType(session, declarationStorage)
                )
            } else {
                IrGetClassImpl(
                    startOffset, endOffset, irType,
                    argument.toIrExpression()
                )
            }
        }
    }

    override fun visitArraySetCall(arraySetCall: FirArraySetCall, data: Any?): IrElement {
        return arraySetCall.convertWithOffsets { startOffset, endOffset ->
            IrErrorCallExpressionImpl(
                startOffset, endOffset, unitType,
                "FirArraySetCall (resolve isn't supported yet)"
            )
        }
    }

    override fun visitResolvedQualifier(resolvedQualifier: FirResolvedQualifier, data: Any?): IrElement {
        val classId = resolvedQualifier.classId
        if (classId != null) {
            val classSymbol = ConeClassLikeLookupTagImpl(classId).toSymbol(session)!!
            return resolvedQualifier.convertWithOffsets { startOffset, endOffset ->
                IrGetObjectValueImpl(
                    startOffset, endOffset,
                    resolvedQualifier.typeRef.toIrType(session, declarationStorage),
                    classSymbol.toIrSymbol(session, declarationStorage) as IrClassSymbol
                )
            }
        }
        return visitElement(resolvedQualifier, data)
    }

    override fun visitBinaryLogicExpression(binaryLogicExpression: FirBinaryLogicExpression, data: Any?): IrElement {
        return binaryLogicExpression.convertWithOffsets<IrElement> { startOffset, endOffset ->
            val leftOperand = binaryLogicExpression.leftOperand.accept(this, data) as IrExpression
            val rightOperand = binaryLogicExpression.rightOperand.accept(this, data) as IrExpression
            when (binaryLogicExpression.kind) {
                LogicOperationKind.AND -> {
                    IrIfThenElseImpl(startOffset, endOffset, irBuiltIns.booleanType, IrStatementOrigin.ANDAND).apply {
                        branches.add(IrBranchImpl(leftOperand, rightOperand))
                        branches.add(elseBranch(constFalse(rightOperand.startOffset, rightOperand.endOffset)))
                    }
                }
                LogicOperationKind.OR -> {
                    IrIfThenElseImpl(startOffset, endOffset, irBuiltIns.booleanType, IrStatementOrigin.OROR).apply {
                        branches.add(IrBranchImpl(leftOperand, constTrue(leftOperand.startOffset, leftOperand.endOffset)))
                        branches.add(elseBranch(rightOperand))
                    }
                }
            }
        }
    }
}
