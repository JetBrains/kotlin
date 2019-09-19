/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.nextgen

import org.jetbrains.kotlin.backend.common.descriptors.*
import org.jetbrains.kotlin.backend.common.serialization.UniqId
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.EmptyPackageFragmentDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.SourceManager
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.IrAnonymousInitializerSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrFileSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrTypeParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.*
import org.jetbrains.kotlin.ir.util.NaiveSourceBasedFileEntryImpl
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedTypeParameterDescriptor
import org.jetbrains.kotlin.types.Variance

class SmartIrProtoReaderImpl(byteArray: ByteArray) : AbstractIrSmartProtoReader(byteArray) {
    override fun createIrOperation_block(block: IrBlock) = block

    override fun createIrOperation_break_(break_: IrBreak) = break_

    override fun createIrOperation_call(call: IrCall) = call

    override fun createIrOperation_classReference(classReference: IrClassReference) = classReference

    override fun createIrOperation_composite(composite: IrComposite) = composite

    override fun createIrOperation_const(const: IrConst<*>) = const

    override fun createIrOperation_continue_(continue_: IrContinue) = continue_

    override fun createIrOperation_delegatingConstructorCall(delegatingConstructorCall: IrDelegatingConstructorCall) = delegatingConstructorCall

    override fun createIrOperation_doWhile(doWhile: IrDoWhileLoop) = doWhile

    override fun createIrOperation_enumConstructorCall(enumConstructorCall: IrEnumConstructorCall) = enumConstructorCall

    override fun createIrOperation_functionReference(functionReference: IrFunctionReference) = functionReference

    override fun createIrOperation_getClass(getClass: IrGetClass) = getClass

    override fun createIrOperation_getEnumValue(getEnumValue: IrGetEnumValue) = getEnumValue

    override fun createIrOperation_getField(getField: IrGetField) = getField

    override fun createIrOperation_getObject(getObject: IrGetObjectValue) = getObject

    override fun createIrOperation_getValue(getValue: IrGetValue) = getValue

    override fun createIrOperation_instanceInitializerCall(instanceInitializerCall: IrInstanceInitializerCall) = instanceInitializerCall

    override fun createIrOperation_propertyReference(propertyReference: IrPropertyReference) = propertyReference

    override fun createIrOperation_return_(return_: IrReturn) = return_

    override fun createIrOperation_setField(setField: IrSetField) = setField

    override fun createIrOperation_setVariable(setVariable: IrSetVariable) = setVariable

    override fun createIrOperation_stringConcat(stringConcat: IrStringConcatenation) = stringConcat

    override fun createIrOperation_throw_(throw_: IrThrow) = throw_

    override fun createIrOperation_try_(try_: IrTry) = try_

    override fun createIrOperation_typeOp(typeOp: IrTypeOperatorCall) = typeOp

    override fun createIrOperation_vararg(vararg: IrVararg) = vararg

    override fun createIrOperation_when_(when_: IrWhen) = when_

    override fun createIrOperation_while_(while_: IrWhileLoop) = while_

    override fun createIrOperation_dynamicMember(dynamicMember: IrDynamicMemberExpression) = dynamicMember

    override fun createIrOperation_dynamicOperator(dynamicOperator: IrDynamicOperatorExpression) = dynamicOperator

    override fun createIrOperation_localDelegatedPropertyReference(localDelegatedPropertyReference: IrLocalDelegatedPropertyReference) = localDelegatedPropertyReference

    override fun createIrOperation_constructorCall(constructorCall: IrConstructorCall) = constructorCall

    override fun createIrOperation_functionExpression(functionExpression: IrFunctionExpression) = functionExpression

    override fun createIrDeclaration_irAnonymousInit(irAnonymousInit: IrAnonymousInitializer) = irAnonymousInit

    override fun createIrDeclaration_irClass(irClass: IrClass) = irClass

    override fun createIrDeclaration_irConstructor(irConstructor: IrConstructor) = irConstructor

    override fun createIrDeclaration_irEnumEntry(irEnumEntry: IrEnumEntry) = irEnumEntry

    override fun createIrDeclaration_irField(irField: IrField) = irField

    override fun createIrDeclaration_irFunction(irFunction: IrSimpleFunction) = irFunction

    override fun createIrDeclaration_irProperty(irProperty: IrProperty) = irProperty

    override fun createIrDeclaration_irTypeParameter(irTypeParameter: IrTypeParameter) = irTypeParameter

    override fun createIrDeclaration_irVariable(irVariable: IrVariable) = irVariable

    override fun createIrDeclaration_irValueParameter(irValueParameter: IrValueParameter) = irValueParameter

    override fun createIrDeclaration_irLocalDelegatedProperty(irLocalDelegatedProperty: IrLocalDelegatedProperty) = irLocalDelegatedProperty

    override fun createIrDeclaration_irTypeAlias(irTypeAlias: IrTypeAlias) = irTypeAlias

    override fun createIrStatement_declaration(coordinates: CoordinatesCarrier, declaration: IrDeclaration): IrStatement {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createIrStatement_expression(coordinates: CoordinatesCarrier, expression: IrExpression): IrStatement {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createIrStatement_blockBody(coordinates: CoordinatesCarrier, blockBody: IrBlockBody): IrStatement {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createIrStatement_branch(coordinates: CoordinatesCarrier, branch: IrBranch): IrStatement {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createIrStatement_catch(coordinates: CoordinatesCarrier, catch: IrCatch): IrStatement {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createIrStatement_syntheticBody(coordinates: CoordinatesCarrier, syntheticBody: IrSyntheticBodyKind): IrStatement {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createDescriptorReference(
        packageFqName: FqName,
        classFqName: FqName,
        name: Int,
        uniqId: UniqId?,
        isGetter: Boolean?,
        isSetter: Boolean?,
        isBackingField: Boolean?,
        isFakeOverride: Boolean?,
        isDefaultConstructor: Boolean?,
        isEnumEntry: Boolean?,
        isEnumSpecial: Boolean?,
        isTypeParameter: Boolean?
    ): DeclarationDescriptor {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun loadString(id: Int): String {
        TODO("JKJ")
    }

    private fun loadType(id: Int): IrType {
        TODO("jkjkk")
    }

    private fun loadSymbol(id: Int): IrSymbol {
        TODO("kkl")
    }

    private fun loadStatementBody(id: Int): IrElement {
        TODO("")
    }

    private fun loadExpressionBody(id: Int): IrExpression {
        TODO("kjkjk")
    }

    private fun getLoopById(id: Int): IrLoop {
        TODO("")
    }

    private fun delayedStart(): Int = TODO("start")
    private fun delayedEnd(): Int = TODO("end")
    private fun delayedType(): IrType = TODO("type")

    private val moduleDescriptor: ModuleDescriptor get() = TODO("moduleDescriptor")
    private val symbolTable: SymbolTable get() = TODO("SymbolTable")

    override fun createUniqId(index: Long, isLocal: Boolean) = UniqId(index, isLocal)

    override fun createCoordinates(startOffset: Int, endOffset: Int) = CoordinatesCarrier(startOffset, endOffset)

    private val visibilities = listOf(
        Visibilities.PUBLIC, Visibilities.PRIVATE, Visibilities.PRIVATE_TO_THIS, Visibilities.PROTECTED, Visibilities
            .INTERNAL, Visibilities.INVISIBLE_FAKE, Visibilities.LOCAL
    )

    override fun createVisibility(name: Int) = visibilities[name]

    override fun createIrStatementOrigin(name: Int): IrStatementOrigin {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createKnownOrigin(index: Int): IrDeclarationOrigin {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createIrDeclarationOrigin_custom(custom: Int): IrDeclarationOrigin {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createIrDeclarationOrigin_origin(origin: IrDeclarationOrigin): IrDeclarationOrigin {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createIrDataIndex(index: Int) = index

    override fun createFqName(segment: List<Int>): FqName =
        if (segment.isEmpty()) FqName.ROOT else FqName(segment.joinToString(".") { loadString(it) })

    override fun createIrDeclarationContainer(declaration: List<IrDeclaration>) = declaration

    override fun createFileEntry(name: String, lineStartOffsets: List<Int>): SourceManager.FileEntry {
        return NaiveSourceBasedFileEntryImpl(name, lineStartOffsets.toIntArray())
    }

    override fun createIrFile(
        declarationId: List<UniqId>,
        fileEntry: SourceManager.FileEntry,
        fqName: FqName,
        annotations: List<IrConstructorCall>,
        explicitlyExportedToCompiler: List<Int>
    ): IrFile {
        val packageFragmentDescriptor = EmptyPackageFragmentDescriptor(moduleDescriptor, fqName)

        val symbol = IrFileSymbolImpl(packageFragmentDescriptor)

        // TODO: Create file deserializer here

        return IrFileImpl(fileEntry, symbol, fqName)
    }

    override fun createStringTable(strings: List<String>): Array<String> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createIrSymbolKind(index: Int) = index

    override fun createIrSymbolData(
        kind: Int,
        uniqId: UniqId,
        topLevelUniqId: UniqId,
        fqname: FqName?,
        descriptorReference: DeclarationDescriptor?
    ): IrSymbol {
        TODO("jkjkkjk")
    }

    override fun createIrSymbolTable(symbols: List<IrSymbol>): Array<IrSymbol> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createIrTypeVariance(index: Int) = Variance.values()[index]

    override fun createAnnotations(annotation: List<IrConstructorCall>) = annotation

    override fun createTypeArguments(typeArgument: List<Int>) = typeArgument.map { loadType(it) }

    override fun createIrStarProjection(void: Boolean?) = IrStarProjectionImpl

    override fun createIrTypeProjection(variance: Variance, type: Int): IrTypeProjection {
        return makeTypeProjection(loadType(type), variance)
    }

    override fun createIrTypeArgument_star(star: IrStarProjection) = star

    override fun createIrTypeArgument_type(type: IrTypeProjection) = type

    override fun createIrSimpleType(
        annotations: List<IrConstructorCall>,
        classifier: Int,
        hasQuestionMark: Boolean,
        argument: List<IrTypeArgument>,
        abbreviation: IrTypeAbbreviation?
    ): IrSimpleType {
        return IrSimpleTypeImpl(loadSymbol(classifier) as IrClassifierSymbol, hasQuestionMark, argument, annotations, abbreviation)
    }

    override fun createIrTypeAbbreviation(
        annotations: List<IrConstructorCall>,
        typeAlias: Int,
        hasQuestionMark: Boolean,
        argument: List<IrTypeArgument>
    ): IrTypeAbbreviation {
        return IrTypeAbbreviationImpl(loadSymbol(typeAlias) as IrTypeAliasSymbol, hasQuestionMark, argument, annotations)
    }

    override fun createIrDynamicType(annotations: List<IrConstructorCall>): IrDynamicType {
        return IrDynamicTypeImpl(null, annotations, Variance.INVARIANT)
    }

    override fun createIrErrorType(annotations: List<IrConstructorCall>): IrErrorType {
        return IrErrorTypeImpl(null, annotations, Variance.INVARIANT)
    }

    override fun createIrType_dynamic(dynamic: IrDynamicType) = dynamic

    override fun createIrType_error(error: IrErrorType) = error

    override fun createIrType_simple(simple: IrSimpleType) = simple

    override fun createIrTypeTable(types: List<IrType>): Array<IrType> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createIrBreak(loopId: Int, label: Int?): IrBreak {
        return IrBreakImpl(delayedStart(), delayedEnd(), delayedType(), getLoopById(loopId)).also {
            it.label = label?.let { l -> loadString(l) }
        }
    }

    override fun createIrBlock(origin: IrStatementOrigin?, statement: List<IrStatement>): IrBlock {
        return IrBlockImpl(delayedStart(), delayedEnd(), delayedType(), origin, statement)
    }

    override fun createMemberAccessCommon(
        dispatchReceiver: IrExpression?,
        extensionReceiver: IrExpression?,
        valueArgument: List<IrExpression>,
        typeArguments: List<IrType>
    ): MemberAccessCarrier {
        return MemberAccessCarrier(dispatchReceiver, extensionReceiver, valueArgument, typeArguments)
    }

    override fun createIrCall(symbol: Int, memberAccess: MemberAccessCarrier, super_: Int?, origin: IrStatementOrigin?): IrCall {
        val functionSymbol = loadSymbol(symbol) as IrSimpleFunctionSymbol
        val superSymbol = super_?.let { loadSymbol(it) as IrClassSymbol }
        return IrCallImpl(
            delayedStart(),
            delayedEnd(),
            delayedType(),
            functionSymbol,
            functionSymbol.descriptor,
            memberAccess.typeArguments.size,
            memberAccess.valueArguments.size,
            origin,
            superSymbol
        ).apply {
            dispatchReceiver = memberAccess.dispatchReceiver
            extensionReceiver = memberAccess.extensionReceiver
            memberAccess.typeArguments.forEachIndexed { index, type -> putTypeArgument(index, type) }
            memberAccess.valueArguments.forEachIndexed { index, value -> putValueArgument(index, value) }
        }
    }

    override fun createIrConstructorCall(
        symbol: Int,
        constructorTypeArgumentsCount: Int,
        memberAccess: MemberAccessCarrier
    ): IrConstructorCall {
        val constructorSymbol = loadSymbol(symbol) as IrConstructorSymbol
        return IrConstructorCallImpl(
            delayedStart(),
            delayedEnd(),
            delayedType(),
            constructorSymbol,
            constructorSymbol.descriptor,
            memberAccess.typeArguments.size,
            constructorTypeArgumentsCount,
            memberAccess.valueArguments.size
        ).apply {
            dispatchReceiver = memberAccess.dispatchReceiver
            extensionReceiver = memberAccess.extensionReceiver
            memberAccess.typeArguments.forEachIndexed { index, type -> putTypeArgument(index, type) }
            memberAccess.valueArguments.forEachIndexed { index, value -> putValueArgument(index, value) }
        }
    }

    override fun createIrFunctionReference(
        symbol: Int,
        origin: IrStatementOrigin?,
        memberAccess: MemberAccessCarrier
    ): IrFunctionReference {
        val functionSymbol = loadSymbol(symbol) as IrFunctionSymbol
        val callable = IrFunctionReferenceImpl(
            delayedStart(),
            delayedEnd(),
            delayedType(),
            functionSymbol,
            functionSymbol.descriptor,
            memberAccess.typeArguments.size,
            memberAccess.valueArguments.size,
            origin
        ).apply {
            dispatchReceiver = memberAccess.dispatchReceiver
            extensionReceiver = memberAccess.extensionReceiver
            memberAccess.typeArguments.forEachIndexed { index, type -> putTypeArgument(index, type) }
            memberAccess.valueArguments.forEachIndexed { index, value -> putValueArgument(index, value) }
        }

        return callable
    }

    override fun createIrLocalDelegatedPropertyReference(
        delegate: Int,
        getter: Int?,
        setter: Int?,
        symbol: Int,
        origin: IrStatementOrigin?
    ): IrLocalDelegatedPropertyReference {
        val delagateSymbol = loadSymbol(delegate) as IrVariableSymbol
        val getterSymbol = getter?.let { loadSymbol(it) as IrSimpleFunctionSymbol } ?: error("For some reason expected to be not-null")
        val setterSymbol = setter?.let { loadSymbol(it) as IrSimpleFunctionSymbol }
        val propertySymbol = loadSymbol(symbol) as IrLocalDelegatedPropertySymbol
        return IrLocalDelegatedPropertyReferenceImpl(delayedStart(), delayedEnd(), delayedType(), propertySymbol, delagateSymbol, getterSymbol, setterSymbol, origin)
    }

    override fun createIrPropertyReference(
        field: Int?,
        getter: Int?,
        setter: Int?,
        origin: IrStatementOrigin?,
        memberAccess: MemberAccessCarrier,
        symbol: Int
    ): IrPropertyReference {
        val propertySymbol = loadSymbol(symbol) as IrPropertySymbol

        val fieldSymbol = field?.let { loadSymbol(it) as IrFieldSymbol }
        val getterSymbol = getter?.let { loadSymbol(it) as IrSimpleFunctionSymbol }
        val setterSymbol = setter?.let { loadSymbol(it) as IrSimpleFunctionSymbol }

        return IrPropertyReferenceImpl(
            delayedStart(), delayedEnd(), delayedType(),
            propertySymbol,
            memberAccess.typeArguments.size,
            fieldSymbol,
            getterSymbol,
            setterSymbol,
            origin
        ).apply {
            dispatchReceiver = memberAccess.dispatchReceiver
            extensionReceiver = memberAccess.extensionReceiver
            memberAccess.typeArguments.forEachIndexed { index, type -> putTypeArgument(index, type) }
            memberAccess.valueArguments.forEachIndexed { index, value -> putValueArgument(index, value) }
        }
    }

    override fun createIrComposite(statement: List<IrStatement>, origin: IrStatementOrigin?): IrComposite {
        return IrCompositeImpl(delayedStart(), delayedEnd(), delayedType(), origin, statement)
    }

    override fun createIrClassReference(classSymbol: Int, classType: Int): IrClassReference {
        val symbol = loadSymbol(classSymbol) as IrClassSymbol
        val type = loadType(classSymbol)
        return IrClassReferenceImpl(delayedStart(), delayedEnd(), delayedType(), symbol, type)
    }

    override fun createIrConst_boolean(boolean: Boolean) = IrConstImpl.boolean(delayedStart(), delayedEnd(), delayedType(), boolean)
    override fun createIrConst_byte(byte: Int) = IrConstImpl.byte(delayedStart(), delayedEnd(), delayedType(), byte.toByte())
    override fun createIrConst_char(char: Int) = IrConstImpl.char(delayedStart(), delayedEnd(), delayedType(), char.toChar())
    override fun createIrConst_double(double: Double) = IrConstImpl.double(delayedStart(), delayedEnd(), delayedType(), double)
    override fun createIrConst_float(float: Float) = IrConstImpl.float(delayedStart(), delayedEnd(), delayedType(), float)
    override fun createIrConst_int(int: Int) = IrConstImpl.int(delayedStart(), delayedEnd(), delayedType(), int)
    override fun createIrConst_long(long: Long) = IrConstImpl.long(delayedStart(), delayedEnd(), delayedType(), long)
    override fun createIrConst_null_(null_: Boolean) = IrConstImpl.constNull(delayedStart(), delayedEnd(), delayedType())
    override fun createIrConst_short(short: Int) = IrConstImpl.short(delayedStart(), delayedEnd(), delayedType(), short.toShort())
    override fun createIrConst_string(string: Int) = IrConstImpl.string(delayedStart(), delayedEnd(), delayedType(), loadString(string))

    override fun createIrContinue(loopId: Int, label: Int?): IrContinue {
        return IrContinueImpl(delayedStart(), delayedEnd(), delayedType(), getLoopById(loopId)).also {
            it.label = label?.let { l -> loadString(l) }
        }
    }

    override fun createIrDelegatingConstructorCall(symbol: Int, memberAccess: MemberAccessCarrier): IrDelegatingConstructorCall {
        val constructorSymbol = loadSymbol(symbol) as IrConstructorSymbol
        return IrDelegatingConstructorCallImpl(delayedStart(), delayedEnd(), delayedType(), constructorSymbol, constructorSymbol.descriptor, memberAccess.typeArguments.size, memberAccess.valueArguments.size).apply {
            dispatchReceiver = memberAccess.dispatchReceiver
            extensionReceiver = memberAccess.extensionReceiver
            memberAccess.typeArguments.forEachIndexed { index, type -> putTypeArgument(index, type) }
            memberAccess.valueArguments.forEachIndexed { index, value -> putValueArgument(index, value) }
        }
    }

    override fun createIrDoWhile(loop: LoopCarrier): IrDoWhileLoop {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createIrEnumConstructorCall(symbol: Int, memberAccess: MemberAccessCarrier): IrEnumConstructorCall {
        val constructorSymbol = loadSymbol(symbol) as IrConstructorSymbol
        return IrEnumConstructorCallImpl(delayedStart(), delayedEnd(), delayedType(), constructorSymbol, memberAccess.typeArguments.size, memberAccess.valueArguments.size).apply {
            dispatchReceiver = memberAccess.dispatchReceiver
            extensionReceiver = memberAccess.extensionReceiver
            memberAccess.typeArguments.forEachIndexed { index, type -> putTypeArgument(index, type) }
            memberAccess.valueArguments.forEachIndexed { index, value -> putValueArgument(index, value) }
        }
    }

    override fun createIrGetClass(argument: IrExpression): IrGetClass {
        return IrGetClassImpl(delayedStart(), delayedEnd(), delayedType(), argument)
    }

    override fun createIrGetEnumValue(symbol: Int): IrGetEnumValue {
        val enumEntrySymbol = loadSymbol(symbol) as IrEnumEntrySymbol
        return IrGetEnumValueImpl(delayedStart(), delayedEnd(), delayedType(), enumEntrySymbol)
    }

    override fun createFieldAccessCommon(symbol: Int, super_: Int?, receiver: IrExpression?): FieldAccessCarrier {
        return FieldAccessCarrier(symbol, super_, receiver)
    }

    override fun createIrGetField(fieldAccess: FieldAccessCarrier, origin: IrStatementOrigin?): IrGetField {
        val fieldSymbol = loadSymbol(fieldAccess.symbolId) as IrFieldSymbol
        val superSymbol = fieldAccess.superId?.let { loadSymbol(it) as IrClassSymbol }
        return IrGetFieldImpl(delayedStart(), delayedEnd(), fieldSymbol, delayedType(), fieldAccess.receiver, origin, superSymbol)
    }

    override fun createIrGetValue(symbol: Int, origin: IrStatementOrigin?): IrGetValue {
        val valueSymbol = loadSymbol(symbol) as IrValueSymbol
        return IrGetValueImpl(delayedStart(), delayedEnd(), delayedType(), valueSymbol, origin)
    }

    override fun createIrGetObject(symbol: Int): IrGetObjectValue {
        val objectSymbol = loadSymbol(symbol) as IrClassSymbol
        return IrGetObjectValueImpl(delayedStart(), delayedEnd(), delayedType(), objectSymbol)
    }

    override fun createIrInstanceInitializerCall(symbol: Int): IrInstanceInitializerCall {
        val classSymbol = loadSymbol(symbol) as IrClassSymbol
        return IrInstanceInitializerCallImpl(delayedStart(), delayedEnd(), classSymbol, delayedType())
    }

    override fun createLoop(
        loopId: Int,
        condition: IrExpression,
        label: Int?,
        body: IrExpression?,
        origin: IrStatementOrigin?
    ): LoopCarrier {
        return LoopCarrier(loopId, condition, label, body, origin)
    }

    override fun createIrReturn(returnTarget: Int, value: IrExpression): IrReturn {
        val returnSymbol = loadSymbol(returnTarget) as IrReturnTargetSymbol
        return IrReturnImpl(delayedStart(), delayedEnd(), delayedType(), returnSymbol, value)
    }

    override fun createIrSetField(fieldAccess: FieldAccessCarrier, value: IrExpression, origin: IrStatementOrigin?): IrSetField {
        val fieldSymbol = loadSymbol(fieldAccess.symbolId) as IrFieldSymbol
        val superSymbol = fieldAccess.superId?.let { loadSymbol(it) as IrClassSymbol }
        return IrSetFieldImpl(delayedStart(), delayedEnd(), fieldSymbol, fieldAccess.receiver, value, delayedType(), origin, superSymbol)
    }

    override fun createIrSetVariable(symbol: Int, value: IrExpression, origin: IrStatementOrigin?): IrSetVariable {
        val variableSymbol = loadSymbol(symbol) as IrVariableSymbol
        return IrSetVariableImpl(delayedStart(), delayedEnd(), delayedType(), variableSymbol, value, origin)
    }

    override fun createIrSpreadElement(expression: IrExpression, coordinates: CoordinatesCarrier): IrSpreadElement {
        return IrSpreadElementImpl(coordinates.start, coordinates.end, expression)
    }

    override fun createIrStringConcat(argument: List<IrExpression>): IrStringConcatenation {
        return IrStringConcatenationImpl(delayedStart(), delayedEnd(), delayedType(), argument)
    }

    override fun createIrThrow(value: IrExpression): IrThrow {
        return IrThrowImpl(delayedStart(), delayedEnd(), delayedType(), value)
    }

    override fun createIrTry(result: IrExpression, catch: List<IrStatement>, finally: IrExpression?): IrTry {
        return IrTryImpl(delayedStart(), delayedEnd(), delayedType(), result, catch as List<IrCatch>, finally)
    }

    override fun createIrTypeOp(operator: IrTypeOperator, operand: Int, argument: IrExpression): IrTypeOperatorCall {
        return IrTypeOperatorCallImpl(delayedStart(), delayedEnd(), delayedType(), operator, loadType(operand), argument)
    }

    override fun createIrVararg(elementType: Int, element: List<IrVarargElement>): IrVararg {
        return IrVarargImpl(delayedStart(), delayedEnd(), delayedType(), loadType(elementType), element)
    }

    override fun createIrVarargElement_expression(expression: IrExpression) = expression

    override fun createIrVarargElement_spreadElement(spreadElement: IrSpreadElement) = spreadElement

    override fun createIrWhen(branch: List<IrStatement>, origin: IrStatementOrigin?): IrWhen {
        return IrWhenImpl(delayedStart(), delayedEnd(), delayedType(), origin, branch as List<IrBranch>)
    }

    override fun createIrWhile(loop: LoopCarrier): IrWhileLoop {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createIrFunctionExpression(function: IrSimpleFunction, origin: IrStatementOrigin): IrFunctionExpression {
        return IrFunctionExpressionImpl(delayedStart(), delayedEnd(), delayedType(), function, origin)
    }

    override fun createIrDynamicMemberExpression(memberName: Int, receiver: IrExpression): IrDynamicMemberExpression {
        return IrDynamicMemberExpressionImpl(delayedStart(), delayedEnd(), delayedType(), loadString(memberName), receiver)
    }

    override fun createIrDynamicOperator(index: Int): IrDynamicOperator {
        return IrDynamicOperator.values()[index]
    }

    override fun createIrDynamicOperatorExpression(
        operator: IrDynamicOperator,
        receiver: IrExpression,
        argument: List<IrExpression>
    ): IrDynamicOperatorExpression {
        return IrDynamicOperatorExpressionImpl(delayedStart(), delayedEnd(), delayedType(), operator).apply {
            this.receiver = receiver
            this.arguments += argument
        }
    }

    override fun createIrTypeOperator(index: Int): IrTypeOperator {
        return IrTypeOperator.values()[index]
    }

    override fun createIrExpression(operation: IrExpression, type: Int, coordinates: CoordinatesCarrier): IrExpression {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createNullableIrExpression(expression: IrExpression?): IrExpression {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createIrDeclarationBase(
        symbol: Int,
        origin: IrDeclarationOrigin,
        coordinates: CoordinatesCarrier,
        annotations: List<IrConstructorCall>
    ): DeclarationBaseCarrier {
        return DeclarationBaseCarrier(symbol, origin, coordinates, annotations)
    }

    override fun createIrFunctionBase(
        base: DeclarationBaseCarrier,
        name: Int,
        visibility: Visibility,
        isInline: Boolean,
        isExternal: Boolean,
        typeParameters: List<IrTypeParameter>,
        dispatchReceiver: IrValueParameter?,
        extensionReceiver: IrValueParameter?,
        valueParameter: List<IrValueParameter>,
        body: Int?,
        returnType: Int
    ): FunctionBaseCarrier {
        return FunctionBaseCarrier(
            base,
            name,
            visibility,
            isInline,
            isExternal,
            typeParameters,
            dispatchReceiver,
            extensionReceiver,
            valueParameter,
            body,
            returnType
        )
    }

    override fun createIrFunction(
        base: FunctionBaseCarrier,
        modality: Modality,
        isTailrec: Boolean,
        isSuspend: Boolean,
        overridden: List<Int>
    ): IrSimpleFunction {
        val start = base.declarationBaseCarrier.coordinates.start
        val end = base.declarationBaseCarrier.coordinates.end
        val origin = base.declarationBaseCarrier.origin
        val functionSymbol = loadSymbol(base.declarationBaseCarrier.symbolId) as IrSimpleFunctionSymbol
        val name = Name.guessByFirstCharacter(loadString(base.nameId))
        val returnType = loadType(base.returnTypeIndex)
        val visibility = base.visibility
        val bodyIndex = base.bodyIndex

        return IrFunctionImpl(start, end, origin, functionSymbol, name, visibility, modality, returnType, base.isInline, base.isExternal, isTailrec, isSuspend).apply {
            annotations.addAll(base.declarationBaseCarrier.annotations)
            overridden.mapTo(overriddenSymbols) { loadSymbol(it) as IrSimpleFunctionSymbol }

            dispatchReceiverParameter = base.dispathReceiver
            extensionReceiverParameter = base.extensionReceiver
            valueParameters.addAll(base.valueParameters)
            typeParameters.addAll(base.typeParameters)

            body = bodyIndex?.let { loadStatementBody(it) as IrBody }

//            (descriptor as? WrappedSimpleFunctionDescriptor).bind(this)
        }
    }

    override fun createIrConstructor(base: FunctionBaseCarrier, isPrimary: Boolean): IrConstructor {
        val start = base.declarationBaseCarrier.coordinates.start
        val end = base.declarationBaseCarrier.coordinates.end
        val origin = base.declarationBaseCarrier.origin
        val constructorSymbol = loadSymbol(base.declarationBaseCarrier.symbolId) as IrConstructorSymbol
        val name = Name.special(loadString(base.nameId))
        val returnType = loadType(base.returnTypeIndex)
        val visibility = base.visibility
        val bodyIndex = base.bodyIndex

        return IrConstructorImpl(start, end, origin, constructorSymbol, name, visibility, returnType, base.isInline, base.isExternal, isPrimary).apply {
            annotations.addAll(base.declarationBaseCarrier.annotations)
            dispatchReceiverParameter = base.dispathReceiver
            extensionReceiverParameter = base.extensionReceiver
            valueParameters.addAll(base.valueParameters)
            typeParameters.addAll(base.typeParameters)

            body = bodyIndex?.let { loadStatementBody(it) as IrBody }

            // (descriptor as? WrappedClassConstructorDescriptor)?.bind(this)
        }
    }

    override fun createIrField(
        base: DeclarationBaseCarrier,
        initializer: Int?,
        name: Int,
        visibility: Visibility,
        isFinal: Boolean,
        isExternal: Boolean,
        isStatic: Boolean,
        type: Int
    ): IrField {
        val start = base.coordinates.start
        val end = base.coordinates.end
        val origin = base.origin
        val fieldSymbol = loadSymbol(base.symbolId) as IrFieldSymbol
        val fieldName = Name.guessByFirstCharacter(loadString(name))
        val fieldType = loadType(type)
        return IrFieldImpl(start, end, origin, fieldSymbol, fieldName, fieldType, visibility, isFinal, isExternal, isStatic).apply {
            this.annotations.addAll(base.annotations)
            this.initializer = initializer?.let { IrExpressionBodyImpl(loadExpressionBody(it)) }

//            (descriptor as? WrappedFieldDescriptor)?.bind(this)
        }
    }

    override fun createIrLocalDelegatedProperty(
        base: DeclarationBaseCarrier,
        name: Int,
        type: Int,
        isVar: Boolean,
        delegate: IrVariable,
        getter: IrSimpleFunction?,
        setter: IrSimpleFunction?
    ): IrLocalDelegatedProperty {
        val start = base.coordinates.start
        val end = base.coordinates.end
        val origin = base.origin
        val propertySymbol = loadSymbol(base.symbolId) as IrLocalDelegatedPropertySymbol
        val propertyType = loadType(type)
        val propertyName = Name.guessByFirstCharacter(loadString(name))

        return IrLocalDelegatedPropertyImpl(start, end, origin, propertySymbol, propertyName, propertyType, isVar).apply {
            this.annotations.addAll(base.annotations)
            this.delegate = delegate
            this.getter = getter ?: error("Should be non-null for some reason")
            this.setter = setter

//            (descriptor as? WrappedVariableDescriptorWithAccessor)?.bind(this)
        }
    }

    override fun createIrProperty(
        base: DeclarationBaseCarrier,
        name: Int,
        visibility: Visibility,
        modality: Modality,
        isVar: Boolean,
        isConst: Boolean,
        isLateinit: Boolean,
        isDelegated: Boolean,
        isExternal: Boolean,
        backingField: IrField?,
        getter: IrSimpleFunction?,
        setter: IrSimpleFunction?
    ): IrProperty {
        val start = base.coordinates.start
        val end = base.coordinates.end
        val origin = base.origin
        val propertySymbol = loadSymbol(base.symbolId) as IrPropertySymbol
        val propertyName = Name.guessByFirstCharacter(loadString(name))

        return IrPropertyImpl(start, end, origin, propertySymbol, propertyName, visibility, modality, isVar, isConst, isLateinit, isDelegated, isExternal).apply {
            this.annotations.addAll(base.annotations)
            this.backingField = backingField?.also { it.correspondingPropertySymbol = propertySymbol }
            this.getter = getter?.also { it.correspondingPropertySymbol = propertySymbol }
            this.setter = setter?.also { it.correspondingPropertySymbol = propertySymbol }

//            (descriptor as? WrappedPropertyDescriptor)?.bind(this)
        }
    }

    override fun createIrVariable(
        base: DeclarationBaseCarrier,
        name: Int,
        type: Int,
        isVar: Boolean,
        isConst: Boolean,
        isLateinit: Boolean,
        initializer: IrExpression?
    ): IrVariable {
        val start = base.coordinates.start
        val end = base.coordinates.end
        val origin = base.origin
        val variableSymbol = loadSymbol(base.symbolId) as IrVariableSymbol
        val variableName = Name.guessByFirstCharacter(loadString(name))

        return IrVariableImpl(start, end, origin, variableSymbol, variableName, loadType(type), isVar, isConst, isLateinit).apply {
            this.annotations.addAll(base.annotations)
            this.initializer = initializer
        }
    }

    override fun createClassKind(index: Int): ClassKind {
        return ClassKind.values()[index]
    }

    override fun createModalityKind(index: Int): Modality {
        return Modality.values()[index]
    }

    override fun createIrValueParameter(
        base: DeclarationBaseCarrier,
        name: Int,
        index: Int,
        type: Int,
        varargElementType: Int?,
        isCrossinline: Boolean,
        isNoinline: Boolean,
        defaultValue: Int?
    ): IrValueParameter {
        val start = base.coordinates.start
        val end = base.coordinates.end
        val origin = base.origin
        val valueSymbol = loadSymbol(base.symbolId) as IrValueParameterSymbol
        val valueName = Name.guessByFirstCharacter(loadString(name))
        val valueType = loadType(type)
        val valueVarargType = varargElementType?.let { loadType(it) }
        return IrValueParameterImpl(start, end, origin, valueSymbol, valueName, index, valueType, valueVarargType, isCrossinline, isNoinline).apply {
            this.annotations.addAll(base.annotations)
            this.defaultValue = defaultValue?.let { IrExpressionBodyImpl(loadExpressionBody(it)) }

//            (descriptor as? WrappedValueParameterDescriptor)?.bind(this)
//            (descriptor as? WrappedReceiverParameterDescriptor)?.bind(this)
        }

    }

    override fun createIrTypeParameter(
        base: DeclarationBaseCarrier,
        name: Int,
        index: Int,
        variance: Variance,
        superType: List<Int>,
        isReified: Boolean
    ): IrTypeParameter {
        val start = base.coordinates.start
        val end = base.coordinates.end
        val origin = base.origin
        val typeParameterSymbol = loadSymbol(base.symbolId) as IrTypeParameterSymbol
        val typeParameterName = Name.guessByFirstCharacter(loadString(name))
        val typeParameterDescriptor = typeParameterSymbol.descriptor

        return if (typeParameterDescriptor is DeserializedTypeParameterDescriptor && typeParameterDescriptor.containingDeclaration is PropertyDescriptor && typeParameterSymbol.isBound) {
            // TODO: Get rid of once new properties are implemented
            IrTypeParameterImpl(start, end, origin, IrTypeParameterSymbolImpl(typeParameterDescriptor), typeParameterName, index, isReified, variance)
        } else {
            symbolTable.declareGlobalTypeParameter(start, end, origin, typeParameterDescriptor) {
                IrTypeParameterImpl(start, end, origin, it, typeParameterName, index, isReified, variance)
            }
        }.apply {
            this.annotations.addAll(base.annotations)
            superType.mapTo(this.superTypes) { loadType(it) }

            (typeParameterDescriptor as? WrappedTypeParameterDescriptor)?.bind(this)
        }
    }

    override fun createIrTypeParameterContainer(typeParameter: List<IrTypeParameter>) = typeParameter

    override fun createIrClass(
        base: DeclarationBaseCarrier,
        name: Int,
        kind: ClassKind,
        visibility: Visibility,
        modality: Modality,
        isCompanion: Boolean,
        isInner: Boolean,
        isData: Boolean,
        isExternal: Boolean,
        isInline: Boolean,
        thisReceiver: IrValueParameter?,
        typeParameters: List<IrTypeParameter>,
        declarationContainer: List<IrDeclaration>,
        superType: List<Int>
    ): IrClass {
        val start = base.coordinates.start
        val end = base.coordinates.end
        val origin = base.origin
        val classSymbol = loadSymbol(base.symbolId) as IrClassSymbol
        val className = Name.guessByFirstCharacter(loadString(name))

        return IrClassImpl(start, end, origin, classSymbol, className, kind, visibility, modality, isCompanion, isInner, isData, isExternal, isInline).apply {
            this.annotations.addAll(base.annotations)
            superType.mapTo(this.superTypes) { loadType(it) }
            this.thisReceiver = thisReceiver
            this.typeParameters.addAll(typeParameters)
            this.declarations.addAll(declarationContainer)

//            (descriptor as? WrappedClassDescriptor)?.bind(this)
        }
    }

    override fun createIrTypeAlias(
        base: DeclarationBaseCarrier,
        name: Int,
        visibility: Visibility,
        typeParameters: List<IrTypeParameter>,
        expandedType: Int,
        isActual: Boolean
    ): IrTypeAlias {
        val start = base.coordinates.start
        val end = base.coordinates.end
        val origin = base.origin
        val typeAliasSymbol = loadSymbol(base.symbolId) as IrTypeAliasSymbol
        val typeAliasName = Name.guessByFirstCharacter(loadString(name))

        return IrTypeAliasImpl(start, end, typeAliasSymbol, typeAliasName, visibility, loadType(expandedType), isActual, origin).apply {
            this.annotations.addAll(base.annotations)
            this.typeParameters.addAll(typeParameters)

//            (descriptor as? WrappedTypeAliasDescriptor)?.bind(this)
        }
    }

    override fun createIrEnumEntry(base: DeclarationBaseCarrier, initializer: Int?, correspondingClass: IrClass?, name: Int): IrEnumEntry {
        val start = base.coordinates.start
        val end = base.coordinates.end
        val origin = base.origin
        val enumEntrySymbol = loadSymbol(base.symbolId) as IrEnumEntrySymbol
        val enumEntryName = Name.guessByFirstCharacter(loadString(name))

        return IrEnumEntryImpl(start, end, origin, enumEntrySymbol, enumEntryName).apply {
            this.annotations.addAll(base.annotations)
            this.correspondingClass = correspondingClass
            this.initializerExpression = initializer?.let { loadExpressionBody(it) }

//            (descriptor as? WrappedEnumEntryDescriptor)?.bind(this)
        }
    }

    override fun createIrAnonymousInit(base: DeclarationBaseCarrier, body: Int): IrAnonymousInitializer {
        val start = base.coordinates.start
        val end = base.coordinates.end
        val origin = base.origin
        val initSymbol = loadSymbol(base.symbolId) as IrAnonymousInitializerSymbol

        return IrAnonymousInitializerImpl(start, end, origin, initSymbol).apply {
            this.annotations.addAll(base.annotations)
            this.body = loadStatementBody(body) as IrBlockBody
        }
    }

    override fun createIrBranch(condition: IrExpression, result: IrExpression): IrBranch {
        return IrBranchImpl(delayedStart(), delayedEnd(), condition, result)
    }

    override fun createIrBlockBody(statement: List<IrStatement>): IrBlockBody {
        return IrBlockBodyImpl(delayedStart(), delayedEnd(), statement)
    }

    override fun createIrCatch(catchParameter: IrVariable, result: IrExpression): IrCatch {
        return IrCatchImpl(delayedStart(), delayedEnd(), catchParameter, result)
    }

    override fun createIrSyntheticBodyKind(index: Int): IrSyntheticBodyKind {
        return IrSyntheticBodyKind.values()[index]
    }

    override fun createIrSyntheticBody(kind: IrSyntheticBodyKind): IrSyntheticBodyKind {
        TODO("jkkjk")
//        return IrSyntheticBodyImpl(delayedStart(), delayedEnd(), kind)
    }
}