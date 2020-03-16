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
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirElseIfTrueCondition
import org.jetbrains.kotlin.fir.expressions.impl.FirNoReceiverExpression
import org.jetbrains.kotlin.fir.expressions.impl.FirUnitExpression
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.FirSuperReference
import org.jetbrains.kotlin.fir.resolve.firSymbolProvider
import org.jetbrains.kotlin.fir.resolve.isIteratorNext
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.resolve.transformers.IntegerLiteralTypeApproximationTransformer
import org.jetbrains.kotlin.fir.scopes.impl.FirIntegerOperator
import org.jetbrains.kotlin.fir.symbols.StandardClassIds
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitor
import org.jetbrains.kotlin.fir.visitors.transformSingle
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFieldImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import java.util.*

class Fir2IrVisitor(
    private val converter: Fir2IrConverter,
    private val components: Fir2IrComponents,
    fakeOverrideMode: FakeOverrideMode
) : Fir2IrComponents by components, FirDefaultVisitor<IrElement, Any?>(), IrGeneratorContextInterface {
    companion object {
        private val NEGATED_OPERATIONS: Set<FirOperation> = EnumSet.of(FirOperation.NOT_EQ, FirOperation.NOT_IDENTITY)

        private val UNARY_OPERATIONS: Set<FirOperation> = EnumSet.of(FirOperation.EXCL)
    }

    private val integerApproximator = IntegerLiteralTypeApproximationTransformer(session.firSymbolProvider, session.inferenceContext)

    private val conversionScope = Fir2IrConversionScope()

    private val fakeOverrideGenerator = Fir2IrFakeOverrideGenerator(session, declarationStorage, conversionScope, fakeOverrideMode)

    private fun FirTypeRef.toIrType(): IrType = with(typeConverter) { toIrType() }

    private fun ConeKotlinType.toIrType(): IrType = with(typeConverter) { toIrType() }

    private fun <T : IrDeclaration> applyParentFromStackTo(declaration: T): T = conversionScope.applyParentFromStackTo(declaration)

    override fun visitElement(element: FirElement, data: Any?): IrElement {
        TODO("Should not be here: ${element.render()}")
    }

    override fun visitFile(file: FirFile, data: Any?): IrFile {
        return conversionScope.withParent(declarationStorage.getIrFile(file)) {
            file.declarations.forEach {
                it.toIrDeclaration()
            }

            annotations = file.annotations.mapNotNull {
                it.accept(this@Fir2IrVisitor, data) as? IrConstructorCall
            }
        }
    }

    private fun FirDeclaration.toIrDeclaration(): IrDeclaration? {
        if (this is FirTypeAlias) return null
        return accept(this@Fir2IrVisitor, null) as IrDeclaration
    }

    // ==================================================================================

    override fun visitEnumEntry(enumEntry: FirEnumEntry, data: Any?): IrElement {
        val irEnumEntry = classifierStorage.getCachedIrEnumEntry(enumEntry)!!
        val correspondingClass = irEnumEntry.correspondingClass ?: return irEnumEntry
        declarationStorage.enterScope(irEnumEntry.descriptor)
        classifierStorage.putEnumEntryClassInScope(enumEntry, correspondingClass)
        converter.processAnonymousObjectMembers(enumEntry.initializer as FirAnonymousObject, correspondingClass)
        conversionScope.withParent(correspondingClass) {
            setClassContent(enumEntry.initializer as FirAnonymousObject)
            irEnumEntry.initializerExpression = IrExpressionBodyImpl(
                IrEnumConstructorCallImpl(
                    startOffset, endOffset, enumEntry.returnTypeRef.toIrType(),
                    correspondingClass.constructors.first().symbol
                )
            )
        }
        declarationStorage.leaveScope(irEnumEntry.descriptor)
        return irEnumEntry
    }

    private fun IrClass.setClassContent(klass: FirClass<*>) {
        declarationStorage.enterScope(descriptor)
        conversionScope.withClass(this) {
            val primaryConstructor = klass.getPrimaryConstructorIfAny()
            val irPrimaryConstructor = primaryConstructor?.let { declarationStorage.getCachedIrConstructor(it)!! }
            if (irPrimaryConstructor != null) {
                with(declarationStorage) {
                    enterScope(irPrimaryConstructor.descriptor)
                    irPrimaryConstructor.valueParameters.forEach { symbolTable.introduceValueParameter(it) }
                    irPrimaryConstructor.putParametersInScope(primaryConstructor)
                    irPrimaryConstructor.setFunctionContent(irPrimaryConstructor.descriptor, primaryConstructor)
                }
            }
            val processedCallableNames = mutableListOf<Name>()
            klass.declarations.forEach {
                if (it !is FirConstructor || !it.isPrimary) {
                    it.toIrDeclaration() ?: return@forEach
                    when (it) {
                        is FirSimpleFunction -> processedCallableNames += it.name
                        is FirProperty -> processedCallableNames += it.name
                    }
                }
            }
            with(fakeOverrideGenerator) { addFakeOverrides(klass, processedCallableNames) }
            annotations = klass.annotations.mapNotNull {
                it.accept(this@Fir2IrVisitor, null) as? IrConstructorCall
            }
            if (irPrimaryConstructor != null) {
                declarationStorage.leaveScope(irPrimaryConstructor.descriptor)
            }
        }
        declarationStorage.leaveScope(descriptor)
    }

    override fun visitRegularClass(regularClass: FirRegularClass, data: Any?): IrElement {
        if (regularClass.visibility == Visibilities.LOCAL) {
            val irParent = conversionScope.parentFromStack()
            val irClass = classifierStorage.getCachedIrClass(regularClass)?.apply { this.parent = irParent }
            if (irClass != null) {
                converter.processRegisteredLocalClassAndNestedClasses(regularClass, irClass)
                return conversionScope.withParent(irClass) {
                    setClassContent(regularClass)
                }
            }
            converter.processLocalClassAndNestedClasses(regularClass, irParent)
        }
        val irClass = classifierStorage.getCachedIrClass(regularClass)!!
        return conversionScope.withParent(irClass) {
            setClassContent(regularClass)
        }
    }

    override fun visitAnonymousObject(anonymousObject: FirAnonymousObject, data: Any?): IrElement {
        val irParent = conversionScope.parentFromStack()
        // NB: for implicit types it is possible that anonymous object is already cached
        val irAnonymousObject = classifierStorage.getCachedIrClass(anonymousObject)?.apply { this.parent = irParent }
            ?: classifierStorage.createIrAnonymousObject(anonymousObject, irParent = irParent)
        converter.processAnonymousObjectMembers(anonymousObject, irAnonymousObject)
        conversionScope.withParent(irAnonymousObject) {
            setClassContent(anonymousObject)
        }
        val anonymousClassType = irAnonymousObject.thisReceiver!!.type
        return anonymousObject.convertWithOffsets { startOffset, endOffset ->
            IrBlockImpl(
                startOffset, endOffset, anonymousClassType, IrStatementOrigin.OBJECT_LITERAL,
                listOf(
                    irAnonymousObject,
                    IrConstructorCallImpl.fromSymbolOwner(
                        startOffset,
                        endOffset,
                        anonymousClassType,
                        irAnonymousObject.constructors.first().symbol,
                        irAnonymousObject.typeParameters.size,
                        origin = IrStatementOrigin.OBJECT_LITERAL
                    )
                )
            )
        }
    }

    // ==================================================================================

    private fun <T : IrFunction> T.setFunctionContent(descriptor: FunctionDescriptor, firFunction: FirFunction<*>?): T {
        conversionScope.withParent(this) {
            if (firFunction != null) {
                if (this !is IrConstructor || !this.isPrimary) {
                    // Scope for primary constructor should be entered before class declaration processing
                    with(declarationStorage) {
                        enterScope(descriptor)
                        valueParameters.forEach { symbolTable.introduceValueParameter(it) }
                        putParametersInScope(firFunction)
                    }
                }
                for ((valueParameter, firValueParameter) in valueParameters.zip(firFunction.valueParameters)) {
                    valueParameter.setDefaultValue(firValueParameter)
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
        val irConstructor = declarationStorage.getCachedIrConstructor(constructor)!!
        return conversionScope.withFunction(irConstructor) {
            setFunctionContent(irConstructor.descriptor, constructor)
        }
    }

    override fun visitAnonymousInitializer(anonymousInitializer: FirAnonymousInitializer, data: Any?): IrElement {
        val irAnonymousInitializer = declarationStorage.getCachedIrAnonymousInitializer(anonymousInitializer)!!
        declarationStorage.enterScope(irAnonymousInitializer.descriptor)
        irAnonymousInitializer.body = anonymousInitializer.body!!.convertToIrBlockBody()
        declarationStorage.leaveScope(irAnonymousInitializer.descriptor)
        return irAnonymousInitializer
    }

    private fun FirDelegatedConstructorCall.toIrDelegatingConstructorCall(): IrCallWithIndexedArgumentsBase? {
        val constructedIrType = constructedTypeRef.toIrType()
        val constructorSymbol = (this.calleeReference as? FirResolvedNamedReference)?.resolvedSymbol as? FirConstructorSymbol
            ?: return null
        return convertWithOffsets { startOffset, endOffset ->
            val irConstructorSymbol = declarationStorage.getIrFunctionSymbol(constructorSymbol) as IrConstructorSymbol
            if (constructorSymbol.fir.isFromEnumClass || constructorSymbol.fir.returnTypeRef.isEnum) {
                IrEnumConstructorCallImpl(
                    startOffset, endOffset,
                    constructedIrType,
                    irConstructorSymbol
                ).apply {
                    val typeArguments = (constructedTypeRef as? FirResolvedTypeRef)?.type?.typeArguments
                    if (typeArguments?.isNotEmpty() == true) {
                        val irType = (typeArguments.first() as ConeKotlinTypeProjection).type.toIrType()
                        putTypeArgument(0, irType)
                    }
                }
            } else {
                IrDelegatingConstructorCallImpl(
                    startOffset, endOffset,
                    constructedIrType,
                    irConstructorSymbol
                )
            }.apply {
                for ((index, argument) in arguments.withIndex()) {
                    val argumentExpression = argument.toIrExpression()
                    putValueArgument(index, argumentExpression)
                }
            }
        }
    }

    override fun visitSimpleFunction(simpleFunction: FirSimpleFunction, data: Any?): IrElement {
        val irFunction = if (simpleFunction.visibility == Visibilities.LOCAL) {
            val irParent = conversionScope.parent()
            declarationStorage.createIrFunction(simpleFunction, irParent)
        } else {
            declarationStorage.getCachedIrFunction(simpleFunction)!!
        }
        return conversionScope.withFunction(irFunction) {
            setFunctionContent(irFunction.descriptor, simpleFunction)
        }
    }

    override fun visitAnonymousFunction(anonymousFunction: FirAnonymousFunction, data: Any?): IrElement {
        return anonymousFunction.convertWithOffsets { startOffset, endOffset ->
            val irFunction = declarationStorage.createIrFunction(anonymousFunction, conversionScope.parent())
            conversionScope.withFunction(irFunction) {
                setFunctionContent(irFunction.descriptor, anonymousFunction)
            }

            val type = anonymousFunction.typeRef.toIrType()

            IrFunctionExpressionImpl(startOffset, endOffset, type, irFunction, IrStatementOrigin.LAMBDA)
        }
    }

    private fun IrValueParameter.setDefaultValue(firValueParameter: FirValueParameter) {
        val firDefaultValue = firValueParameter.defaultValue
        if (firDefaultValue != null) {
            this.defaultValue = IrExpressionBodyImpl(firDefaultValue.toIrExpression())
        }
    }

    private fun visitLocalVariable(variable: FirProperty): IrElement {
        assert(variable.isLocal)
        val initializer = variable.initializer
        val isNextVariable = initializer is FirFunctionCall &&
                initializer.resolvedNamedFunctionSymbol()?.callableId?.isIteratorNext() == true &&
                variable.source.psi?.parent is KtForExpression
        val irVariable = declarationStorage.createIrVariable(
            variable, conversionScope.parentFromStack(), if (isNextVariable) IrDeclarationOrigin.FOR_LOOP_VARIABLE else null
        )
        if (initializer != null) {
            irVariable.initializer = initializer.toIrExpression()
        }
        return irVariable
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
        val inferredType = type ?: firInitializerExpression!!.typeRef.toIrType()
        val irField = symbolTable.declareField(
            startOffset, endOffset, origin, descriptor, inferredType
        ) { symbol ->
            IrFieldImpl(
                startOffset, endOffset, origin, symbol,
                name, inferredType,
                visibility, isFinal = isFinal, isExternal = false,
                isStatic = property.isStatic || parent !is IrClass,
                isFakeOverride = origin == IrDeclarationOrigin.FAKE_OVERRIDE
            )
        }
        return conversionScope.withParent(applyParentFromStackTo(irField)) {
            declarationStorage.enterScope(descriptor)
            val initializerExpression = firInitializerExpression?.toIrExpression()
            initializer = initializerExpression?.let { IrExpressionBodyImpl(it) }
            correspondingPropertySymbol = this@createBackingField.symbol
            declarationStorage.leaveScope(descriptor)
        }
    }

    private fun IrProperty.setPropertyContent(descriptor: PropertyDescriptor, property: FirProperty): IrProperty {
        val initializer = property.initializer
        val delegate = property.delegate
        val irParent = this.parent
        val propertyType = property.returnTypeRef.toIrType()
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
                    property, IrDeclarationOrigin.PROPERTY_DELEGATE, descriptor,
                    Visibilities.PRIVATE, Name.identifier("${property.name}\$delegate"), true, delegate
                )
            }
        }
        getter?.setPropertyAccessorContent(
            property.getter, this, propertyType, property.getter is FirDefaultPropertyGetter
        )
        if (property.isVar) {
            setter?.setPropertyAccessorContent(
                property.setter, this, propertyType, property.setter is FirDefaultPropertySetter
            )
        }
        annotations = property.annotations.mapNotNull {
            it.accept(this@Fir2IrVisitor, null) as? IrConstructorCall
        }
        return this
    }

    override fun visitProperty(property: FirProperty, data: Any?): IrElement {
        if (property.isLocal) return visitLocalVariable(property)
        val irProperty = declarationStorage.getCachedIrProperty(property)!!
        return conversionScope.withProperty(irProperty) {
            setPropertyContent(irProperty.descriptor, property)
        }
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
        isDefault: Boolean
    ) {
        conversionScope.withFunction(this) {
            applyParentFromStackTo(this)
            setFunctionContent(descriptor, propertyAccessor)
            if (isDefault) {
                conversionScope.withParent(this) {
                    declarationStorage.enterScope(descriptor)
                    val backingField = correspondingProperty.backingField
                    val fieldSymbol = symbolTable.referenceField(correspondingProperty.descriptor)
                    val declaration = this
                    if (backingField != null) {
                        body = IrBlockBodyImpl(
                            startOffset, endOffset,
                            listOf(
                                if (isSetter) {
                                    IrSetFieldImpl(startOffset, endOffset, fieldSymbol, typeConverter.unitType).apply {
                                        setReceiver(declaration)
                                        value = IrGetValueImpl(startOffset, endOffset, propertyType, valueParameters.first().symbol)
                                    }
                                } else {
                                    IrReturnImpl(
                                        startOffset, endOffset, typeConverter.nothingType, symbol,
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
        val irTarget = conversionScope.returnTarget(returnExpression)
        return returnExpression.convertWithOffsets { startOffset, endOffset ->
            val result = returnExpression.result
            val descriptor = irTarget.descriptor
            IrReturnImpl(
                startOffset, endOffset, typeConverter.nothingType,
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

    override fun visitVarargArgumentsExpression(varargArgumentsExpression: FirVarargArgumentsExpression, data: Any?): IrElement {
        val irReturnType = varargArgumentsExpression.typeRef.toIrType()
        return IrVarargImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, irReturnType,
                            varargArgumentsExpression.varargElementType.toIrType(),
                            varargArgumentsExpression.arguments.map { arg ->
                                arg.toIrExpression().run {
                                    if (arg is FirSpreadArgumentExpression) IrSpreadElementImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, this)
                                    else this
                                }
                            })
    }

    private fun FirQualifiedAccess.toIrExpression(typeRef: FirTypeRef): IrExpression {
        val type = typeRef.toIrType()
        val symbol = calleeReference.toSymbol(session, classifierStorage, declarationStorage)
        return typeRef.convertWithOffsets { startOffset, endOffset ->
            if (calleeReference is FirSuperReference) {
                if (typeRef !is FirComposedSuperTypeRef) {
                    val dispatchReceiver = conversionScope.lastDispatchReceiverParameter()
                    if (dispatchReceiver != null) {
                        return@convertWithOffsets IrGetValueImpl(startOffset, endOffset, dispatchReceiver.type, dispatchReceiver.symbol)
                    }
                }
            }
            when {
                symbol is IrConstructorSymbol -> IrConstructorCallImpl.fromSymbolOwner(startOffset, endOffset, type, symbol)
                symbol is IrSimpleFunctionSymbol -> IrCallImpl(
                    startOffset, endOffset, type, symbol, origin = calleeReference.statementOrigin()
                )
                symbol is IrPropertySymbol && symbol.isBound -> {
                    val getter = symbol.owner.getter
                    if (getter != null) {
                        IrCallImpl(startOffset, endOffset, type, getter.symbol, origin = IrStatementOrigin.GET_PROPERTY)
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
        val type = coneType?.toIrType()
        val symbol = type?.classifierOrNull
        return convertWithOffsets { startOffset, endOffset ->
            when (symbol) {
                is IrClassSymbol -> {
                    val irClass = symbol.owner
                    val fir = firSymbol?.fir as? FirClass<*>
                    val irConstructor = fir?.getPrimaryConstructorIfAny()?.let { firConstructor ->
                        declarationStorage.getIrConstructorSymbol(firConstructor.symbol)
                    }
                    if (irConstructor == null) {
                        IrErrorCallExpressionImpl(startOffset, endOffset, type, "No annotation constructor found: ${irClass.name}")
                    } else {
                        IrConstructorCallImpl.fromSymbolDescriptor(startOffset, endOffset, type, irConstructor)
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
                        val argumentMapping = call.argumentMapping
                        if (argumentMapping != null && argumentMapping.isNotEmpty()) {
                            require(call is FirFunctionCall)
                            val function =
                                ((call.calleeReference as? FirResolvedNamedReference)?.resolvedSymbol as? FirFunctionSymbol<*>)?.fir
                            val valueParameters = function?.valueParameters
                            if (valueParameters != null) {
                                for ((argument, parameter) in argumentMapping) {
                                    val argumentExpression = argument.toIrExpression()
                                    putValueArgument(valueParameters.indexOf(parameter), argumentExpression)
                                }
                                return this
                            }
                        }
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

    private fun IrExpression.applyTypeArguments(access: FirQualifiedAccess): IrExpression {
        return when (this) {
            is IrCallWithIndexedArgumentsBase -> {
                val argumentsCount = access.typeArguments.size
                if (argumentsCount <= typeArgumentsCount) {
                    apply {
                        for ((index, argument) in access.typeArguments.withIndex()) {
                            val argumentIrType = (argument as FirTypeProjectionWithVariance).typeRef.toIrType()
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
        if (firReceiver is FirResolvedQualifier) return firReceiver.toGetObject()
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
                    val irClass = classifierStorage.getCachedIrClass(firClass)!!
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
                }
                if (ownerFunction.extensionReceiverParameter != null) {
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

    override fun visitFunctionCall(functionCall: FirFunctionCall, data: Any?): IrExpression {
        val convertibleCall = if (functionCall.toResolvedCallableSymbol()?.fir is FirIntegerOperator) {
            functionCall.copy().transformSingle(integerApproximator, null)
        } else {
            functionCall
        }
        return convertibleCall.toIrExpression(convertibleCall.typeRef)
            .applyCallArguments(convertibleCall).applyTypeArguments(convertibleCall).applyReceivers(convertibleCall)
    }

    private fun FirFunctionCall.resolvedNamedFunctionSymbol(): FirNamedFunctionSymbol? {
        val calleeReference = (calleeReference as? FirResolvedNamedReference) ?: return null
        return calleeReference.resolvedSymbol as? FirNamedFunctionSymbol
    }

    override fun visitAnnotationCall(annotationCall: FirAnnotationCall, data: Any?): IrElement {
        return annotationCall.toIrExpression().applyCallArguments(annotationCall)
    }

    override fun visitQualifiedAccessExpression(qualifiedAccessExpression: FirQualifiedAccessExpression, data: Any?): IrElement {
        return qualifiedAccessExpression.toIrExpression(qualifiedAccessExpression.typeRef)
            .applyTypeArguments(qualifiedAccessExpression).applyReceivers(qualifiedAccessExpression)
    }

    override fun visitThisReceiverExpression(thisReceiverExpression: FirThisReceiverExpression, data: Any?): IrElement {
        val calleeReference = thisReceiverExpression.calleeReference
        if (calleeReference.labelName == null && calleeReference.boundSymbol is FirRegularClassSymbol) {
            // Object case
            val firObject = (calleeReference.boundSymbol?.fir as? FirClass)?.takeIf {
                it is FirAnonymousObject || it is FirRegularClass && it.classKind == ClassKind.OBJECT
            }
            if (firObject != null) {
                val irObject = classifierStorage.getCachedIrClass(firObject)!!
                if (irObject != conversionScope.lastClass()) {
                    return thisReceiverExpression.convertWithOffsets { startOffset, endOffset ->
                        IrGetObjectValueImpl(startOffset, endOffset, irObject.defaultType, irObject.symbol)
                    }
                }
            }

            val dispatchReceiver = conversionScope.lastDispatchReceiverParameter()
            if (dispatchReceiver != null) {
                return thisReceiverExpression.convertWithOffsets { startOffset, endOffset ->
                    IrGetValueImpl(startOffset, endOffset, dispatchReceiver.type, dispatchReceiver.symbol)
                }
            }
        }
        // TODO handle qualified "this" in instance methods of non-inner classes (inner class cases are handled by InnerClassesLowering)
        return visitQualifiedAccessExpression(thisReceiverExpression, data)
    }

    override fun visitExpressionWithSmartcast(expressionWithSmartcast: FirExpressionWithSmartcast, data: Any?): IrElement {
        // Generate the expression with the original type and then cast it to the smart cast type.
        val value = expressionWithSmartcast.originalExpression.toIrExpression()
        val castType = expressionWithSmartcast.typeRef.toIrType()
        if (value.type == castType) return value
        return IrTypeOperatorCallImpl(
            value.startOffset,
            value.endOffset,
            castType,
            IrTypeOperator.IMPLICIT_CAST,
            castType,
            value
        )
    }

    override fun visitCallableReferenceAccess(callableReferenceAccess: FirCallableReferenceAccess, data: Any?): IrElement {
        val symbol = callableReferenceAccess.calleeReference.toSymbol(session, classifierStorage, declarationStorage)
        val type = callableReferenceAccess.typeRef.toIrType()
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
                        typeArgumentsCount = 0,
                        reflectionTarget = symbol
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
        val symbol = calleeReference.toSymbol(session, classifierStorage, declarationStorage)
        return variableAssignment.convertWithOffsets { startOffset, endOffset ->
            if (symbol != null && symbol.isBound) {
                when (symbol) {
                    is IrFieldSymbol -> IrSetFieldImpl(
                        startOffset, endOffset, symbol, typeConverter.unitType
                    ).apply {
                        value = variableAssignment.rValue.toIrExpression()
                    }
                    is IrPropertySymbol -> {
                        val irProperty = symbol.owner
                        val backingField = irProperty.backingField
                        if (backingField != null) {
                            IrSetFieldImpl(
                                startOffset, endOffset, backingField.symbol, typeConverter.unitType
                            ).apply {
                                value = variableAssignment.rValue.toIrExpression()
                            }
                        } else {
                            generateErrorCallExpression(startOffset, endOffset, calleeReference)
                        }
                    }
                    is IrVariableSymbol -> {
                        IrSetVariableImpl(
                            startOffset, endOffset, irBuiltIns.unitType, symbol, variableAssignment.rValue.toIrExpression(), null
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
                constExpression.typeRef.toIrType(),
                kind, value
            )
        }
    }

    // ==================================================================================

    private fun FirStatement.toIrStatement(): IrStatement? {
        if (this is FirTypeAlias) return null
        if (this is FirUnitExpression) return toIrExpression()
        if (this is FirBlock) return toIrExpression()
        return accept(this@Fir2IrVisitor, null) as IrStatement
    }

    private fun FirExpression.toIrExpression(): IrExpression {
        return when (this) {
            is FirBlock -> convertToIrExpressionOrBlock()
            is FirUnitExpression -> convertWithOffsets { startOffset, endOffset ->
                IrGetObjectValueImpl(
                    startOffset, endOffset, typeConverter.unitType,
                    symbolTable.referenceClass(irBuiltIns.builtIns.unit)
                )
            }
            else -> accept(this@Fir2IrVisitor, null) as IrExpression
        }
    }

    private fun FirBlock.mapToIrStatements(): List<IrStatement?> {
        val irRawStatements = statements.map { it.toIrStatement() }
        val result = mutableListOf<IrStatement?>()
        var missNext = false
        for ((index, irRawStatement) in irRawStatements.withIndex()) {
            if (missNext) {
                missNext = false
                continue
            } else if (irRawStatement is IrVariable && irRawStatement.origin == IrDeclarationOrigin.FOR_LOOP_ITERATOR) {
                missNext = true
                val irNextStatement = irRawStatements[index + 1]!!
                result += IrBlockImpl(
                    irRawStatement.startOffset, irNextStatement.endOffset,
                    (irNextStatement as IrExpression).type, IrStatementOrigin.FOR_LOOP,
                    listOf(irRawStatement, irNextStatement)
                )
            } else {
                result += irRawStatement
            }
        }
        return result
    }

    private fun FirBlock.convertToIrBlockBody(): IrBlockBody {
        return convertWithOffsets { startOffset, endOffset ->
            val irStatements = mapToIrStatements()
            IrBlockBodyImpl(
                startOffset, endOffset,
                if (irStatements.isNotEmpty()) {
                    irStatements.filterNotNull().takeIf { it.isNotEmpty() }
                        ?: listOf(IrBlockImpl(startOffset, endOffset, typeConverter.unitType, null, emptyList()))
                } else {
                    emptyList()
                }
            )
        }
    }

    private fun FirBlock.convertToIrExpressionOrBlock(origin: IrStatementOrigin? = null): IrExpression {
        if (statements.size == 1) {
            val firStatement = statements.single()
            if (firStatement is FirExpression) {
                return firStatement.toIrExpression()
            }
        }
        val type =
            (statements.lastOrNull() as? FirExpression)?.typeRef?.toIrType() ?: typeConverter.unitType
        return convertWithOffsets { startOffset, endOffset ->
            IrBlockImpl(
                startOffset, endOffset, type, origin,
                mapToIrStatements().filterNotNull()
            )
        }
    }

    override fun visitErrorExpression(errorExpression: FirErrorExpression, data: Any?): IrElement {
        return errorExpression.convertWithOffsets { startOffset, endOffset ->
            IrErrorExpressionImpl(
                startOffset, endOffset,
                errorExpression.typeRef.toIrType(),
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
                applyParentFromStackTo(declarationStorage.declareTemporaryVariable(subjectExpression.toIrExpression(), "subject"))
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
        return conversionScope.withSubject(subjectVariable) {
            whenExpression.convertWithOffsets { startOffset, endOffset ->
                val irWhen = IrWhenImpl(
                    startOffset, endOffset,
                    whenExpression.typeRef.toIrType(),
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
                IrElseBranchImpl(IrConstImpl.boolean(irResult.startOffset, irResult.endOffset, typeConverter.booleanType, true), irResult)
            } else {
                IrBranchImpl(startOffset, endOffset, condition.toIrExpression(), irResult)
            }
        }
    }

    override fun visitWhenSubjectExpression(whenSubjectExpression: FirWhenSubjectExpression, data: Any?): IrElement {
        val lastSubjectVariable = conversionScope.lastSubject()
        return whenSubjectExpression.convertWithOffsets { startOffset, endOffset ->
            IrGetValueImpl(startOffset, endOffset, lastSubjectVariable.type, lastSubjectVariable.symbol)
        }
    }

    private val loopMap = mutableMapOf<FirLoop, IrLoop>()

    override fun visitDoWhileLoop(doWhileLoop: FirDoWhileLoop, data: Any?): IrElement {
        return doWhileLoop.convertWithOffsets { startOffset, endOffset ->
            IrDoWhileLoopImpl(
                startOffset, endOffset, typeConverter.unitType,
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
            val origin = if (whileLoop.psi is KtForExpression) IrStatementOrigin.FOR_LOOP_INNER_WHILE
            else IrStatementOrigin.WHILE_LOOP
            IrWhileLoopImpl(startOffset, endOffset, typeConverter.unitType, origin).apply {
                loopMap[whileLoop] = this
                label = whileLoop.label?.name
                condition = whileLoop.condition.toIrExpression()
                body = whileLoop.block.convertToIrExpressionOrBlock(origin)
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
                IrErrorExpressionImpl(startOffset, endOffset, typeConverter.nothingType, "Unbound loop: ${render()}")
            } else {
                f(startOffset, endOffset, irLoop).apply {
                    label = irLoop.label.takeIf { target.labelName != null }
                }
            }
        }
    }

    override fun visitBreakExpression(breakExpression: FirBreakExpression, data: Any?): IrElement {
        return breakExpression.convertJumpWithOffsets { startOffset, endOffset, irLoop ->
            IrBreakImpl(startOffset, endOffset, typeConverter.nothingType, irLoop)
        }
    }

    override fun visitContinueExpression(continueExpression: FirContinueExpression, data: Any?): IrElement {
        return continueExpression.convertJumpWithOffsets { startOffset, endOffset, irLoop ->
            IrContinueImpl(startOffset, endOffset, typeConverter.nothingType, irLoop)
        }
    }

    override fun visitThrowExpression(throwExpression: FirThrowExpression, data: Any?): IrElement {
        return throwExpression.convertWithOffsets { startOffset, endOffset ->
            IrThrowImpl(startOffset, endOffset, typeConverter.nothingType, throwExpression.exception.toIrExpression())
        }
    }

    override fun visitTryExpression(tryExpression: FirTryExpression, data: Any?): IrElement {
        return tryExpression.convertWithOffsets { startOffset, endOffset ->
            IrTryImpl(
                startOffset, endOffset, tryExpression.typeRef.toIrType(),
                tryExpression.tryBlock.convertToIrExpressionOrBlock(),
                tryExpression.catches.map { it.accept(this, data) as IrCatch },
                tryExpression.finallyBlock?.convertToIrExpressionOrBlock()
            )
        }
    }

    override fun visitCatch(catch: FirCatch, data: Any?): IrElement {
        return catch.convertWithOffsets { startOffset, endOffset ->
            val catchParameter = declarationStorage.createIrVariable(catch.parameter, conversionScope.parentFromStack())
            IrCatchImpl(startOffset, endOffset, catchParameter).apply {
                result = catch.block.convertToIrExpressionOrBlock()
            }
        }
    }

    override fun visitComparisonExpression(comparisonExpression: FirComparisonExpression, data: Any?): IrElement {
        return comparisonExpression.convertWithOffsets { startOffset, endOffset ->
            generateComparisonCall(startOffset, endOffset, comparisonExpression)
        }
    }

    private fun generateComparisonCall(
        startOffset: Int, endOffset: Int,
        comparisonExpression: FirComparisonExpression
    ): IrExpression {
        val operation = comparisonExpression.operation

        fun fallbackToRealCall(): IrExpression {
            val (symbol, origin) = getSymbolAndOriginForComparison(operation, irBuiltIns.intType.classifierOrFail)
            return primitiveOp2(
                startOffset, endOffset,
                symbol!!,
                typeConverter.booleanType,
                origin,
                visitFunctionCall(comparisonExpression.compareToCall, null),
                IrConstImpl.int(startOffset, endOffset, irBuiltIns.intType, 0)
            )
        }

        val comparisonInfo = comparisonExpression.inferPrimitiveNumericComparisonInfo() ?: return fallbackToRealCall()
        val comparisonType = comparisonInfo.comparisonType

        // Currently inferPrimitiveNumericComparisonInfo returns null for different primitive values
        // TODO: Support different primitive types as well and fix inferPrimitiveNumericComparisonInfo
        require(comparisonInfo.leftPrimitiveType == comparisonInfo.rightPrimitiveType && comparisonInfo.leftType == comparisonInfo.rightType) {
            "Contract for inferPrimitiveNumericComparisonInfo is violated"
        }

        val simpleType = when ((comparisonType.type as? ConeClassLikeType)?.lookupTag?.classId) {
            StandardClassIds.Long -> irBuiltIns.longType
            StandardClassIds.Int -> irBuiltIns.intType
            StandardClassIds.Float -> irBuiltIns.floatType
            StandardClassIds.Double -> irBuiltIns.doubleType
            StandardClassIds.Char -> irBuiltIns.charType
            else -> return fallbackToRealCall()
        }

        val (symbol, origin) = getSymbolAndOriginForComparison(operation, simpleType.classifierOrFail)

        return primitiveOp2(
            startOffset, endOffset, symbol!!, typeConverter.booleanType, origin,
            comparisonExpression.left.toIrExpression(), comparisonExpression.right.toIrExpression()
        )
    }

    private fun getSymbolAndOriginForComparison(
        operation: FirOperation,
        classifier: IrClassifierSymbol
    ): Pair<IrSimpleFunctionSymbol?, IrStatementOriginImpl> {
        return when (operation) {
            FirOperation.LT -> irBuiltIns.lessFunByOperandType[classifier] to IrStatementOrigin.LT
            FirOperation.GT -> irBuiltIns.greaterFunByOperandType[classifier] to IrStatementOrigin.GT
            FirOperation.LT_EQ -> irBuiltIns.lessOrEqualFunByOperandType[classifier] to IrStatementOrigin.LTEQ
            FirOperation.GT_EQ -> irBuiltIns.greaterOrEqualFunByOperandType[classifier] to IrStatementOrigin.GTEQ
            else -> error("Unexpected comparison operation: $operation")
        }
    }

    private fun generateOperatorCall(
        startOffset: Int, endOffset: Int, operation: FirOperation, arguments: List<FirExpression>
    ): IrExpression {
        val (type, symbol, origin) = when (operation) {
            FirOperation.EQ -> Triple(typeConverter.booleanType, irBuiltIns.eqeqSymbol, IrStatementOrigin.EQEQ)
            FirOperation.NOT_EQ -> Triple(typeConverter.booleanType, irBuiltIns.eqeqSymbol, IrStatementOrigin.EXCLEQ)
            FirOperation.IDENTITY -> Triple(typeConverter.booleanType, irBuiltIns.eqeqeqSymbol, IrStatementOrigin.EQEQEQ)
            FirOperation.NOT_IDENTITY -> Triple(typeConverter.booleanType, irBuiltIns.eqeqeqSymbol, IrStatementOrigin.EXCLEQEQ)
            FirOperation.EXCL -> Triple(typeConverter.booleanType, irBuiltIns.booleanNotSymbol, IrStatementOrigin.EXCL)
            FirOperation.LT, FirOperation.GT,
            FirOperation.LT_EQ, FirOperation.GT_EQ,
            FirOperation.OTHER, FirOperation.ASSIGN, FirOperation.PLUS_ASSIGN,
            FirOperation.MINUS_ASSIGN, FirOperation.TIMES_ASSIGN,
            FirOperation.DIV_ASSIGN, FirOperation.REM_ASSIGN,
            FirOperation.IS, FirOperation.NOT_IS,
            FirOperation.AS, FirOperation.SAFE_AS
            -> {
                TODO("Should not be here: incompatible operation in FirOperatorCall: $operation")
            }
        }
        val result = if (operation in UNARY_OPERATIONS) {
            primitiveOp1(startOffset, endOffset, symbol, type, origin, arguments[0].toIrExpression())
        } else {
            primitiveOp2(startOffset, endOffset, symbol, type, origin, arguments[0].toIrExpression(), arguments[1].toIrExpression())
        }
        if (operation !in NEGATED_OPERATIONS) return result
        return primitiveOp1(startOffset, endOffset, irBuiltIns.booleanNotSymbol, typeConverter.booleanType, origin, result)
    }

    override fun visitOperatorCall(operatorCall: FirOperatorCall, data: Any?): IrElement {
        return operatorCall.convertWithOffsets { startOffset, endOffset ->
            generateOperatorCall(startOffset, endOffset, operatorCall.operation, operatorCall.arguments)
        }
    }

    override fun visitStringConcatenationCall(stringConcatenationCall: FirStringConcatenationCall, data: Any?): IrElement {
        return stringConcatenationCall.convertWithOffsets { startOffset, endOffset ->
            IrStringConcatenationImpl(
                startOffset, endOffset, typeConverter.stringType,
                stringConcatenationCall.arguments.map { it.toIrExpression() }
            )
        }
    }

    override fun visitTypeOperatorCall(typeOperatorCall: FirTypeOperatorCall, data: Any?): IrElement {
        return typeOperatorCall.convertWithOffsets { startOffset, endOffset ->
            val irTypeOperand = typeOperatorCall.conversionTypeRef.toIrType()
            val (irType, irTypeOperator) = when (typeOperatorCall.operation) {
                FirOperation.IS -> typeConverter.booleanType to IrTypeOperator.INSTANCEOF
                FirOperation.NOT_IS -> typeConverter.booleanType to IrTypeOperator.NOT_INSTANCEOF
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
                checkNotNullCall.typeRef.toIrType(),
                irBuiltIns.checkNotNullSymbol,
                IrStatementOrigin.EXCLEXCL
            ).apply {
                putTypeArgument(0, checkNotNullCall.argument.typeRef.toIrType().makeNotNull())
                putValueArgument(0, checkNotNullCall.argument.toIrExpression())
            }
        }
    }

    override fun visitGetClassCall(getClassCall: FirGetClassCall, data: Any?): IrElement {
        val argument = getClassCall.argument
        val irType = getClassCall.typeRef.toIrType()
        val irClassType = argument.typeRef.toIrType()
        val irClassReferenceSymbol = when (argument) {
            is FirResolvedReifiedParameterReference -> {
                classifierStorage.getIrTypeParameterSymbol(argument.symbol, ConversionTypeContext.DEFAULT)
            }
            is FirResolvedQualifier -> {
                val symbol = argument.symbol as? FirClassSymbol
                    ?: return getClassCall.convertWithOffsets { startOffset, endOffset ->
                        IrErrorCallExpressionImpl(
                            startOffset, endOffset, irType, "Resolved qualifier ${argument.render()} does not have correct symbol"
                        )
                    }
                classifierStorage.getIrClassSymbol(symbol)
            }
            else -> null
        }
        return getClassCall.convertWithOffsets { startOffset, endOffset ->
            if (irClassReferenceSymbol != null) {
                IrClassReferenceImpl(startOffset, endOffset, irType, irClassReferenceSymbol, irClassType)
            } else {
                IrGetClassImpl(startOffset, endOffset, irType, argument.toIrExpression())
            }
        }
    }

    override fun visitAugmentedArraySetCall(augmentedArraySetCall: FirAugmentedArraySetCall, data: Any?): IrElement {
        return augmentedArraySetCall.convertWithOffsets { startOffset, endOffset ->
            IrErrorCallExpressionImpl(
                startOffset, endOffset, typeConverter.unitType,
                "FirArraySetCall (resolve isn't supported yet)"
            )
        }
    }

    private fun FirResolvedQualifier.toGetObject(): IrExpression {
        val irType = typeRef.toIrType()
        val typeRef = typeRef as? FirResolvedTypeRef
        val classSymbol = (typeRef?.type as? ConeClassLikeType)?.lookupTag?.toSymbol(session)
        return convertWithOffsets { startOffset, endOffset ->
            if (classSymbol != null) {
                IrGetObjectValueImpl(
                    startOffset, endOffset, irType,
                    classSymbol.toSymbol(session, classifierStorage) as IrClassSymbol
                )
            } else {
                IrErrorCallExpressionImpl(
                    startOffset, endOffset, irType, "Resolved qualifier ${render()} does not have correctly resolved type"
                )
            }
        }
    }

    override fun visitResolvedQualifier(resolvedQualifier: FirResolvedQualifier, data: Any?): IrElement {
        return resolvedQualifier.toGetObject()
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
