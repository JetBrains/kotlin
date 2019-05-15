/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.backend.common.descriptors.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.*
import org.jetbrains.kotlin.fir.descriptors.FirModuleDescriptor
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirElseIfTrueCondition
import org.jetbrains.kotlin.fir.expressions.impl.FirUnitExpression
import org.jetbrains.kotlin.fir.references.FirPropertyFromParameterCallableReference
import org.jetbrains.kotlin.fir.resolve.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.buildUseSiteScope
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.scopes.impl.FirClassSubstitutionScope
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.*
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.primitiveOp1
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.*
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.types.impl.IrErrorTypeImpl
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi2ir.PsiSourceManager
import org.jetbrains.kotlin.types.AbstractStrictEqualityTypeChecker
import org.jetbrains.kotlin.types.Variance
import java.util.*

internal class Fir2IrVisitor(
    private val session: FirSession,
    private val moduleDescriptor: FirModuleDescriptor,
    private val symbolTable: SymbolTable,
    private val sourceManager: PsiSourceManager,
    private val irBuiltIns: IrBuiltIns,
    private val fakeOverrideMode: FakeOverrideMode
) : FirVisitor<IrElement, Any?>() {
    companion object {
        private val NEGATED_OPERATIONS: Set<FirOperation> = EnumSet.of(FirOperation.NOT_EQ, FirOperation.NOT_IDENTITY)

        private val UNARY_OPERATIONS: Set<FirOperation> = EnumSet.of(FirOperation.EXCL)
    }

    private val typeContext = session.typeContext

    private val declarationStorage = Fir2IrDeclarationStorage(session, symbolTable, moduleDescriptor)

    private val nothingType = FirImplicitNothingTypeRef(session, null).toIrType(session, declarationStorage)

    private val unitType = FirImplicitUnitTypeRef(session, null).toIrType(session, declarationStorage)

    private val booleanType = FirImplicitBooleanTypeRef(session, null).toIrType(session, declarationStorage)

    private val stringType = FirImplicitStringTypeRef(session, null).toIrType(session, declarationStorage)

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

    override fun visitElement(element: FirElement, data: Any?): IrElement {
        TODO("Should not be here: ${element.render()}")
    }

    override fun visitFile(file: FirFile, data: Any?): IrFile {
        return IrFileImpl(
            sourceManager.getOrCreateFileEntry(file.psi as KtFile),
            moduleDescriptor.findPackageFragmentForFile(file)
        ).withParent {
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

    private fun FirTypeRef.collectFunctionNamesFromThisAndSupertypes(result: MutableList<Name> = mutableListOf()): List<Name> {
        if (this is FirResolvedTypeRef) {
            val superType = type
            if (superType is ConeClassLikeType) {
                when (val superSymbol = superType.lookupTag.toSymbol(this@Fir2IrVisitor.session)) {
                    is FirClassSymbol -> {
                        val superClass = superSymbol.fir
                        for (declaration in superClass.declarations) {
                            if (declaration is FirNamedFunction) {
                                result += declaration.name
                            }
                        }
                        superClass.collectFunctionNamesFromSupertypes(result)
                    }
                    is FirTypeAliasSymbol -> {
                        val superAlias = superSymbol.fir
                        superAlias.expandedTypeRef.collectFunctionNamesFromThisAndSupertypes(result)
                    }
                }
            }
        }
        return result
    }

    private fun FirClass.collectFunctionNamesFromSupertypes(result: MutableList<Name> = mutableListOf()): List<Name> {
        for (superTypeRef in superTypeRefs) {
            superTypeRef.collectFunctionNamesFromThisAndSupertypes(result)
        }
        return result
    }

    private fun FirClass.getPrimaryConstructorIfAny(): FirConstructor? =
        (declarations.firstOrNull() as? FirConstructor)?.takeIf { it.isPrimary }

    private fun IrClass.addFakeOverrides(klass: FirClass, processedFunctionNames: MutableList<Name>) {
        if (fakeOverrideMode == FakeOverrideMode.NONE) return
        val superTypesFunctionNames = klass.collectFunctionNamesFromSupertypes()
        val useSiteScope = (klass as? FirRegularClass)?.buildUseSiteScope(session, ScopeSession()) ?: return
        for (name in superTypesFunctionNames) {
            if (name in processedFunctionNames) continue
            processedFunctionNames += name
            useSiteScope.processFunctionsByName(name) { functionSymbol ->
                // TODO: think about overloaded functions. May be we should process all names.
                if (functionSymbol is FirFunctionSymbol) {
                    val originalFunction = functionSymbol.fir as FirNamedFunction
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
                        val fakeOverrideSymbol = FirClassSubstitutionScope.createFakeOverride(session, originalFunction, functionSymbol)
                        val fakeOverrideFunction = fakeOverrideSymbol.fir as FirNamedFunction

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
        }
    }

    private fun IrClass.setClassContent(klass: FirClass) {
        for (superTypeRef in klass.superTypeRefs) {
            superTypes += superTypeRef.toIrType(session, declarationStorage)
        }
        if (klass is FirRegularClass) {
            for ((index, typeParameter) in klass.typeParameters.withIndex()) {
                typeParameters += declarationStorage.getIrTypeParameter(typeParameter, index).setParentByParentStack()
            }
        }
        declarationStorage.enterScope(descriptor)
        val primaryConstructor = klass.getPrimaryConstructorIfAny()
        val irPrimaryConstructor = primaryConstructor?.accept(this@Fir2IrVisitor, null) as IrConstructor?
        withClass {
            if (irPrimaryConstructor != null) {
                declarations += irPrimaryConstructor
            }
            val processedFunctionNames = mutableListOf<Name>()
            klass.declarations.forEach {
                if (it !is FirConstructor || !it.isPrimary) {
                    val irDeclaration = it.toIrDeclaration() ?: return@forEach
                    declarations += irDeclaration
                    if (it is FirNamedFunction) {
                        processedFunctionNames += it.name
                    }
                }
            }
            addFakeOverrides(klass, processedFunctionNames)
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

    private fun <T : IrFunction> T.setFunctionContent(
        descriptor: FunctionDescriptor,
        firFunction: FirFunction,
        firOverriddenSymbol: FirFunctionSymbol? = null
    ): T {
        setParentByParentStack()
        withParent {
            if (firFunction is FirNamedFunction) {
                for ((index, typeParameter) in firFunction.typeParameters.withIndex()) {
                    typeParameters += declarationStorage.getIrTypeParameter(typeParameter, index).setParentByParentStack()
                }
            }
            val firFunctionSymbol = (firFunction as? FirNamedFunction)?.symbol
            val lastClass = classStack.lastOrNull()
            val containingClass = if (firOverriddenSymbol == null || firFunctionSymbol == null) {
                lastClass
            } else {
                val callableId = firFunctionSymbol.callableId
                val ownerClassId = callableId.classId
                if (ownerClassId == null) {
                    lastClass
                } else {
                    val classLikeSymbol = session.service<FirSymbolProvider>().getClassLikeSymbolByFqName(ownerClassId)
                    if (classLikeSymbol !is FirClassSymbol) {
                        lastClass
                    } else {
                        val firClass = classLikeSymbol.fir
                        declarationStorage.getIrClass(firClass, setParent = false)
                    }
                }
            }
            if (firFunction !is FirConstructor && containingClass != null) {
                val thisOrigin = IrDeclarationOrigin.DEFINED
                val thisType = containingClass.thisReceiver!!.type
                dispatchReceiverParameter = symbolTable.declareValueParameter(
                    startOffset, endOffset, thisOrigin, WrappedValueParameterDescriptor(),
                    thisType
                ) { symbol ->
                    IrValueParameterImpl(
                        startOffset, endOffset, thisOrigin, symbol,
                        Name.special("<this>"), -1, thisType,
                        varargElementType = null, isCrossinline = false, isNoinline = false
                    ).setParentByParentStack()
                }
            }
            for ((valueParameter, firValueParameter) in valueParameters.zip(firFunction.valueParameters)) {
                valueParameter.setDefaultValue(firValueParameter)
            }
            if (firOverriddenSymbol != null && this is IrSimpleFunction && firFunctionSymbol != null) {
                val overriddenSymbol = declarationStorage.getIrFunctionSymbol(firOverriddenSymbol)
                if (overriddenSymbol is IrSimpleFunctionSymbol) {
                    overriddenSymbols += overriddenSymbol
                }
            }
            body = firFunction.body?.convertToIrBlockBody()
            if (this !is IrConstructor) {
                // Scope for primary constructor should be left after class declaration
                // Scope for secondary constructor should be left after delegating call
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
        }.withParent {
            if (!parentAsClass.isAnnotationClass) {
                val body = this.body as IrBlockBody? ?: IrBlockBodyImpl(startOffset, endOffset)
                val delegatedConstructor = constructor.delegatedConstructor
                if (delegatedConstructor != null) {
                    val irDelegatingConstructorCall = delegatedConstructor.toIrDelegatingConstructorCall()
                    body.statements += irDelegatingConstructorCall ?: delegatedConstructor.convertWithOffsets { startOffset, endOffset ->
                        IrErrorCallExpressionImpl(
                            startOffset, endOffset, irConstructor.returnType, "Cannot find delegated constructor call"
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
            }
            if (!constructor.isPrimary) {
                declarationStorage.leaveScope(irConstructor.descriptor)
            }
        }
    }

    override fun visitAnonymousInitializer(anonymousInitializer: FirAnonymousInitializer, data: Any?): IrElement {
        val origin = IrDeclarationOrigin.DEFINED
        val parent = parentStack.last() as IrClass
        return anonymousInitializer.convertWithOffsets { startOffset, endOffset ->
            symbolTable.declareAnonymousInitializer(
                startOffset, endOffset, origin, parent.descriptor
            ).apply {
                declarationStorage.enterScope(descriptor)
                body = anonymousInitializer.body!!.convertToIrBlockBody()
                declarationStorage.leaveScope(descriptor)
            }
        }
    }

    private fun FirDelegatedConstructorCall.toIrDelegatingConstructorCall(): IrDelegatingConstructorCall? {
        val constructedClassSymbol = with(typeContext) {
            (constructedTypeRef as FirResolvedTypeRef).type.typeConstructor()
        } as? FirClassSymbol ?: return null
        val constructedIrType = constructedTypeRef.toIrType(this@Fir2IrVisitor.session, declarationStorage)
        // TODO: find delegated constructor correctly
        val classId = constructedClassSymbol.classId
        val provider = this@Fir2IrVisitor.session.service<FirSymbolProvider>()
        var constructorSymbol: FirCallableSymbol? = null
        provider.getClassUseSiteMemberScope(classId, this@Fir2IrVisitor.session, ScopeSession())!!.processFunctionsByName(
            classId.shortClassName
        ) {
            if (arguments.size <= ((it as FirFunctionSymbol).fir as FirFunction).valueParameters.size) {
                constructorSymbol = it
                ProcessorAction.STOP
            } else {
                ProcessorAction.NEXT
            }
        }
        if (constructorSymbol == null) return null
        return convertWithOffsets { startOffset, endOffset ->
            IrDelegatingConstructorCallImpl(
                startOffset, endOffset,
                constructedIrType,
                declarationStorage.getIrFunctionSymbol(constructorSymbol as FirFunctionSymbol) as IrConstructorSymbol
            ).apply {
                for ((index, argument) in arguments.withIndex()) {
                    val argumentExpression = argument.toIrExpression()
                    putValueArgument(index, argumentExpression)
                }
            }
        }
    }

    override fun visitNamedFunction(namedFunction: FirNamedFunction, data: Any?): IrElement {
        val irFunction = declarationStorage.getIrFunction(
            namedFunction, irParent = parentStack.last() as? IrClass
        )
        return irFunction.setParentByParentStack().withFunction {
            setFunctionContent(irFunction.descriptor, namedFunction)
        }
    }

    override fun visitAnonymousFunction(anonymousFunction: FirAnonymousFunction, data: Any?): IrElement {
        val irFunction = declarationStorage.getIrLocalFunction(anonymousFunction)
        irFunction.setParentByParentStack().withFunction {
            setFunctionContent(irFunction.descriptor, anonymousFunction)
        }
        return anonymousFunction.convertWithOffsets { startOffset, endOffset ->
            val type = anonymousFunction.typeRef.toIrType(session, declarationStorage)
            val origin = when (anonymousFunction.psi) {
                is KtFunctionLiteral -> IrStatementOrigin.LAMBDA
                else -> IrStatementOrigin.ANONYMOUS_FUNCTION
            }
            IrBlockImpl(
                startOffset, endOffset, type, origin,
                listOf(
                    irFunction, IrFunctionReferenceImpl(
                        startOffset, endOffset, type, irFunction.symbol, irFunction.descriptor, 0, origin
                    )
                )
            )
        }
    }

    private fun IrValueParameter.setDefaultValue(firValueParameter: FirValueParameter) {
        val firDefaultValue = firValueParameter.defaultValue
        if (firDefaultValue != null) {
            this.defaultValue = IrExpressionBodyImpl(firDefaultValue.toIrExpression())
        }
    }

    override fun visitVariable(variable: FirVariable, data: Any?): IrElement {
        val irVariable = declarationStorage.createAndSaveIrVariable(variable)
        return irVariable.setParentByParentStack().apply {
            val initializer = variable.initializer
            if (initializer != null) {
                this.initializer = initializer.toIrExpression()
            }
        }
    }

    private fun IrProperty.setPropertyContent(descriptor: PropertyDescriptor, property: FirProperty): IrProperty {
        val initializer = property.initializer
        val irParent = this.parent
        val type = property.returnTypeRef.toIrType(session, declarationStorage)
        // TODO: this checks are very preliminary, FIR resolve should determine backing field presence itself
        if (property.modality != Modality.ABSTRACT && (irParent !is IrClass || !irParent.isInterface)) {
            if (initializer != null || property.getter is FirDefaultPropertyGetter ||
                property.isVar && property.setter is FirDefaultPropertySetter
            ) {
                val backingOrigin = IrDeclarationOrigin.PROPERTY_BACKING_FIELD
                backingField = symbolTable.declareField(
                    startOffset, endOffset, backingOrigin, descriptor, type
                ) { symbol ->
                    IrFieldImpl(
                        startOffset, endOffset, backingOrigin, symbol,
                        property.name, type, property.visibility,
                        isFinal = property.isVal, isExternal = false,
                        isStatic = property.isStatic || irParent !is IrClass
                    )
                }.setParentByParentStack().withParent {
                    declarationStorage.enterScope(descriptor)
                    val initializerExpression = initializer?.toIrExpression()
                    this.initializer = initializerExpression?.let { IrExpressionBodyImpl(it) }
                    declarationStorage.leaveScope(descriptor)
                }
            }
        }
        getter = property.getter.accept(this@Fir2IrVisitor, type) as IrSimpleFunction
        if (property.isVar) {
            setter = property.setter.accept(this@Fir2IrVisitor, type) as IrSimpleFunction
        }
        property.annotations.forEach {
            annotations += it.accept(this@Fir2IrVisitor, null) as IrConstructorCall
        }
        return this
    }

    override fun visitProperty(property: FirProperty, data: Any?): IrProperty {
        val irProperty = declarationStorage.getIrProperty(property)
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


    private fun createPropertyAccessor(
        propertyAccessor: FirPropertyAccessor, startOffset: Int, endOffset: Int,
        correspondingProperty: IrProperty, isDefault: Boolean, propertyType: IrType
    ): IrSimpleFunction {
        val origin = when {
            isDefault -> IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR
            else -> IrDeclarationOrigin.DEFINED
        }
        val isSetter = propertyAccessor.isSetter
        val prefix = if (isSetter) "set" else "get"
        val descriptor = WrappedSimpleFunctionDescriptor()
        return symbolTable.declareSimpleFunction(
            startOffset, endOffset, origin, descriptor
        ) { symbol ->
            val accessorReturnType = propertyAccessor.returnTypeRef.toIrType(session, declarationStorage)
            IrFunctionImpl(
                startOffset, endOffset, origin, symbol,
                Name.special("<$prefix-${correspondingProperty.name}>"),
                propertyAccessor.visibility, correspondingProperty.modality, accessorReturnType,
                isInline = false, isExternal = false, isTailrec = false, isSuspend = false
            ).withFunction {
                descriptor.bind(this)
                declarationStorage.enterScope(descriptor)
                if (!isDefault) {
                    with(declarationStorage) { declareParameters(propertyAccessor, containingClass = null) }
                }
                setFunctionContent(descriptor, propertyAccessor).apply {
                    correspondingPropertySymbol = symbolTable.referenceProperty(correspondingProperty.descriptor)
                    if (isDefault) {
                        withParent {
                            declarationStorage.enterScope(descriptor)
                            val backingField = correspondingProperty.backingField
                            if (isSetter) {
                                valueParameters += symbolTable.declareValueParameter(
                                    startOffset, endOffset, origin, WrappedValueParameterDescriptor(), propertyType
                                ) { symbol ->
                                    IrValueParameterImpl(
                                        startOffset, endOffset, IrDeclarationOrigin.DEFINED, symbol,
                                        Name.special("<set-?>"), 0, propertyType,
                                        varargElementType = null,
                                        isCrossinline = false, isNoinline = false
                                    ).setParentByParentStack()
                                }
                            }
                            val fieldSymbol = symbolTable.referenceField(correspondingProperty.descriptor)
                            val declaration = this
                            if (backingField != null) {
                                body = IrBlockBodyImpl(
                                    startOffset, endOffset,
                                    listOf(
                                        if (isSetter) {
                                            IrSetFieldImpl(startOffset, endOffset, fieldSymbol, accessorReturnType).apply {
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
        }
    }

    override fun visitPropertyAccessor(propertyAccessor: FirPropertyAccessor, data: Any?): IrElement {
        val correspondingProperty = propertyStack.last()
        return propertyAccessor.convertWithOffsets { startOffset, endOffset ->
            createPropertyAccessor(
                propertyAccessor, startOffset, endOffset, correspondingProperty,
                isDefault = propertyAccessor is FirDefaultPropertyGetter || propertyAccessor is FirDefaultPropertySetter,
                propertyType = data as IrType
            )
        }
    }

    override fun visitReturnExpression(returnExpression: FirReturnExpression, data: Any?): IrElement {
        val firTarget = returnExpression.target.labeledElement
        var irTarget = functionStack.last()
        for (potentialTarget in functionStack.asReversed()) {
            // TODO: remove comparison by name
            if (potentialTarget.name == (firTarget as? FirNamedFunction)?.name) {
                irTarget = potentialTarget
                break
            }
        }
        return returnExpression.convertWithOffsets { startOffset, endOffset ->
            val result = returnExpression.result
            IrReturnImpl(
                startOffset, endOffset, nothingType,
                symbolTable.referenceSimpleFunction(irTarget.descriptor),
                result.toIrExpression()
            )
        }
    }

    override fun visitUncheckedNotNullCast(uncheckedNotNullCast: FirUncheckedNotNullCast, data: Any?): IrElement {
        // TODO: Ensure correct
        return uncheckedNotNullCast.expression.toIrExpression()
    }

    override fun visitWrappedArgumentExpression(wrappedArgumentExpression: FirWrappedArgumentExpression, data: Any?): IrElement {
        // TODO: change this temporary hack to something correct
        return wrappedArgumentExpression.expression.toIrExpression()
    }

    private fun FirQualifiedAccess.toIrExpression(typeRef: FirTypeRef): IrExpression {
        val type = typeRef.toIrType(this@Fir2IrVisitor.session, declarationStorage)
        val symbol = calleeReference.toSymbol(declarationStorage)
        return typeRef.convertWithOffsets { startOffset, endOffset ->
            when {
                symbol is IrConstructorSymbol -> IrConstructorCallImpl.fromSymbolOwner(startOffset, endOffset, type, symbol)
                symbol is IrSimpleFunctionSymbol -> IrCallImpl(startOffset, endOffset, type, symbol)
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
                    if (calleeReference is FirPropertyFromParameterCallableReference) {
                        IrStatementOrigin.INITIALIZE_PROPERTY_FROM_PARAMETER
                    } else null
                )
                else -> IrErrorCallExpressionImpl(startOffset, endOffset, type, "Unresolved reference: ${calleeReference.render()}")
            }
        }
    }

    private fun FirAnnotationCall.toIrExpression(): IrExpression {
        val type = (annotationTypeRef as? FirResolvedTypeRef)?.type?.toIrType(this@Fir2IrVisitor.session, declarationStorage)
        val symbol = type?.classifierOrNull
        return convertWithOffsets { startOffset, endOffset ->
            when (symbol) {
                is IrClassSymbol -> {
                    val irClass = symbol.owner
                    val irConstructor = irClass.constructors.firstOrNull()
                    if (irConstructor == null) {
                        IrErrorCallExpressionImpl(startOffset, endOffset, type, "No annotation constructor found: ${irClass.name}")
                    } else {
                        IrConstructorCallImpl.fromSymbolDescriptor(startOffset, endOffset, type, irConstructor.symbol)
                    }

                }
                else -> IrErrorCallExpressionImpl(startOffset, endOffset, type ?: createErrorType(), "Unresolved reference: ${render()}")
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

    private fun IrExpression.applyReceivers(qualifiedAccess: FirQualifiedAccess): IrExpression {
        return when (this) {
            is IrCallImpl -> {
                val ownerFunction = symbol.owner
                if (ownerFunction.dispatchReceiverParameter != null) {
                    val explicitReceiver = qualifiedAccess.explicitReceiver?.toIrExpression()
                    if (explicitReceiver != null) {
                        dispatchReceiver = explicitReceiver
                    } else {
                        // TODO: implicit dispatch receiver
                    }
                } else if (ownerFunction.extensionReceiverParameter != null) {
                    val explicitReceiver = qualifiedAccess.explicitReceiver?.toIrExpression()
                    if (explicitReceiver != null) {
                        extensionReceiver = explicitReceiver
                    } else {
                        // TODO: implicit extension receiver
                    }
                }
                this
            }
            else -> this
        }
    }

    override fun visitFunctionCall(functionCall: FirFunctionCall, data: Any?): IrElement {
        return functionCall.toIrExpression(functionCall.typeRef).applyCallArguments(functionCall).applyReceivers(functionCall)
    }

    override fun visitAnnotationCall(annotationCall: FirAnnotationCall, data: Any?): IrElement {
        return annotationCall.toIrExpression().applyCallArguments(annotationCall)
    }

    override fun visitQualifiedAccessExpression(qualifiedAccessExpression: FirQualifiedAccessExpression, data: Any?): IrElement {
        return qualifiedAccessExpression.toIrExpression(qualifiedAccessExpression.typeRef).applyReceivers(qualifiedAccessExpression)
    }

    private fun generateErrorCallExpression(startOffset: Int, endOffset: Int, calleeReference: FirReference): IrErrorCallExpression {
        return IrErrorCallExpressionImpl(
            startOffset, endOffset, IrErrorTypeImpl(null, emptyList(), Variance.INVARIANT),
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
                        startOffset, endOffset, symbol, symbol.owner.type
                    ).apply {
                        value = variableAssignment.rValue.toIrExpression()
                    }
                    is IrPropertySymbol -> {
                        val irProperty = symbol.owner
                        val backingField = irProperty.backingField
                        if (backingField != null) {
                            IrSetFieldImpl(
                                startOffset, endOffset, backingField.symbol, backingField.symbol.owner.type
                            ).apply {
                                value = variableAssignment.rValue.toIrExpression()
                            }
                        } else {
                            generateErrorCallExpression(startOffset, endOffset, calleeReference)
                        }
                    }
                    else -> generateErrorCallExpression(startOffset, endOffset, calleeReference)
                }
            } else {
                generateErrorCallExpression(startOffset, endOffset, calleeReference)
            }
        }
    }

    override fun <T> visitConstExpression(constExpression: FirConstExpression<T>, data: Any?): IrElement {
        return constExpression.convertWithOffsets { startOffset, endOffset ->
            IrConstImpl(
                startOffset, endOffset,
                constExpression.typeRef.toIrType(session, declarationStorage),
                constExpression.kind, constExpression.value
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
                        startOffset, endOffset, anonymousClassType, anonymousClass.constructors.first().symbol
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
                errorExpression.reason
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
                condition = doWhileLoop.condition.toIrExpression()
                body = doWhileLoop.block.convertToIrExpressionOrBlock()
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
            ClassId(FqName("kotlin"), FqName("Long"), false) -> irBuiltIns.builtIns.longType
            ClassId(FqName("kotlin"), FqName("Int"), false) -> irBuiltIns.builtIns.intType
            ClassId(FqName("kotlin"), FqName("Float"), false) -> irBuiltIns.builtIns.floatType
            ClassId(FqName("kotlin"), FqName("Double"), false) -> irBuiltIns.builtIns.doubleType
            else -> {
                return IrErrorCallExpressionImpl(
                    startOffset, endOffset, booleanType, "Comparison of arguments with unsupported type: $classId"
                )
            }
        }
        val (symbol, origin) = when (operation) {
            FirOperation.LT -> irBuiltIns.lessFunByOperandType[simpleType] to IrStatementOrigin.LT
            FirOperation.GT -> irBuiltIns.greaterFunByOperandType[simpleType] to IrStatementOrigin.GT
            FirOperation.LT_EQ -> irBuiltIns.lessOrEqualFunByOperandType[simpleType] to IrStatementOrigin.LTEQ
            FirOperation.GT_EQ -> irBuiltIns.greaterOrEqualFunByOperandType[simpleType] to IrStatementOrigin.GTEQ
            else -> throw AssertionError("Unexpected comparison operation: $operation")
        }
        return IrBinaryPrimitiveImpl(
            startOffset, endOffset, booleanType, origin, symbol!!,
            first.toIrExpression(), second.toIrExpression()
        )
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
            IrBinaryPrimitiveImpl(
                startOffset, endOffset, type, origin, symbol,
                arguments[0].toIrExpression(), arguments[1].toIrExpression()
            )
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
                irTypeOperand.classifierOrFail, typeOperatorCall.argument.toIrExpression()
            )
        }
    }

    override fun visitGetClassCall(getClassCall: FirGetClassCall, data: Any?): IrElement {
        return getClassCall.convertWithOffsets { startOffset, endOffset ->
            IrGetClassImpl(
                startOffset, endOffset, getClassCall.typeRef.toIrType(session, declarationStorage),
                getClassCall.argument.toIrExpression()
            )
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
}