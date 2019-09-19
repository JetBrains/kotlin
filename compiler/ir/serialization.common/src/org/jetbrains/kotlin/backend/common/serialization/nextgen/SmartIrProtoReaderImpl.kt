/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.nextgen

import org.jetbrains.kotlin.backend.common.serialization.UniqId
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.EmptyPackageFragmentDescriptor
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.SourceManager
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.IrFileSymbolImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.*
import org.jetbrains.kotlin.ir.util.NaiveSourceBasedFileEntryImpl
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.types.Variance

class SmartIrProtoReaderImpl(byteArray: ByteArray) : AbstractIrSmartProtoReader(byteArray) {
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

    private fun getLoopById(id: Int): IrLoop {
        TODO("")
    }

    private fun delayedStart(): Int = TODO("start")
    private fun delayedEnd(): Int = TODO("end")
    private fun delayedType(): IrType = TODO("type")

    private val moduleDescriptor: ModuleDescriptor get() = TODO("jklkl")

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

    override fun createIrDeclarationOrigin(origin: IrDeclarationOrigin?, custom: Int?): IrDeclarationOrigin {
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

    override fun createIrTypeArgument(star: IrStarProjection?, type: IrTypeProjection?) =
        star ?: type ?: error("Expecting one is non-null")

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

    override fun createIrType(simple: IrSimpleType?, dynamic: IrDynamicType?, error: IrErrorType?): IrType {
        return simple ?: dynamic ?: error ?: error("One and only one should be non-null")
    }

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

    override fun createIrConst(
        null_: Boolean?,
        boolean: Boolean?,
        char: Int?,
        byte: Int?,
        short: Int?,
        int: Int?,
        long: Long?,
        float: Float?,
        double: Double?,
        string: Int?
    ): IrConst<*> {
        val start = delayedStart()
        val end = delayedEnd()
        val type = delayedType()
        null_?.let { return IrConstImpl.constNull(start, end, type) }
        boolean?.let { return IrConstImpl.boolean(start, end, type, it) }
        char?.let { return IrConstImpl.char(start, end, type, it.toChar()) }
        byte?.let { return IrConstImpl.byte(start, end, type, it.toByte()) }
        short?.let { return IrConstImpl.short(start, end, type, it.toShort()) }
        int?.let { return IrConstImpl.int(start, end, type, it) }
        long?.let { return IrConstImpl.long(start, end, type, it) }
        float?.let { return IrConstImpl.float(start, end, type, it) }
        double?.let { return IrConstImpl.double(start, end, type, it) }
        string?.let { return IrConstImpl.string(start, end, type, loadString(it)) }

        error("One and only one should be non-null")
    }

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
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createIrGetField(fieldAccess: FieldAccessCarrier, origin: IrStatementOrigin?): IrGetField {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createIrGetValue(symbol: Int, origin: IrStatementOrigin?): IrGetValue {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createIrGetObject(symbol: Int): IrGetObjectValue {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createIrInstanceInitializerCall(symbol: Int): IrInstanceInitializerCall {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createLoop(
        loopId: Int,
        condition: IrExpression,
        label: Int?,
        body: IrExpression?,
        origin: IrStatementOrigin?
    ): LoopCarrier {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createIrReturn(returnTarget: Int, value: IrExpression): IrReturn {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createIrSetField(fieldAccess: FieldAccessCarrier, value: IrExpression, origin: IrStatementOrigin?): IrSetField {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createIrSetVariable(symbol: Int, value: IrExpression, origin: IrStatementOrigin?): IrSetVariable {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createIrSpreadElement(expression: IrExpression, coordinates: CoordinatesCarrier): IrSpreadElement {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createIrStringConcat(argument: List<IrExpression>): IrStringConcatenation {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createIrThrow(value: IrExpression): IrThrow {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createIrTry(result: IrExpression, catch: List<IrStatement>, finally: IrExpression?): IrTry {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createIrTypeOp(operator: IrTypeOperator, operand: Int, argument: IrExpression): IrTypeOperatorCall {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createIrVararg(elementType: Int, element: List<IrVarargElement>): IrVararg {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createIrVarargElement(expression: IrExpression?, spreadElement: IrSpreadElement?): IrVarargElement {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createIrWhen(branch: List<IrStatement>, origin: IrStatementOrigin?): IrWhen {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createIrWhile(loop: LoopCarrier): IrWhileLoop {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createIrFunctionExpression(function: IrSimpleFunction, origin: IrStatementOrigin): IrFunctionExpression {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createIrDynamicMemberExpression(memberName: Int, receiver: IrExpression): IrDynamicMemberExpression {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createIrDynamicOperator(index: Int): IrDynamicOperator {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createIrDynamicOperatorExpression(
        operator: IrDynamicOperator,
        receiver: IrExpression,
        argument: List<IrExpression>
    ): IrDynamicOperatorExpression {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createIrOperation(
        block: IrBlock?,
        break_: IrBreak?,
        call: IrCall?,
        classReference: IrClassReference?,
        composite: IrComposite?,
        const: IrConst<*>?,
        continue_: IrContinue?,
        delegatingConstructorCall: IrDelegatingConstructorCall?,
        doWhile: IrDoWhileLoop?,
        enumConstructorCall: IrEnumConstructorCall?,
        functionReference: IrFunctionReference?,
        getClass: IrGetClass?,
        getEnumValue: IrGetEnumValue?,
        getField: IrGetField?,
        getObject: IrGetObjectValue?,
        getValue: IrGetValue?,
        instanceInitializerCall: IrInstanceInitializerCall?,
        propertyReference: IrPropertyReference?,
        return_: IrReturn?,
        setField: IrSetField?,
        setVariable: IrSetVariable?,
        stringConcat: IrStringConcatenation?,
        throw_: IrThrow?,
        try_: IrTry?,
        typeOp: IrTypeOperatorCall?,
        vararg: IrVararg?,
        when_: IrWhen?,
        while_: IrWhileLoop?,
        dynamicMember: IrDynamicMemberExpression?,
        dynamicOperator: IrDynamicOperatorExpression?,
        localDelegatedPropertyReference: IrLocalDelegatedPropertyReference?,
        constructorCall: IrConstructorCall?,
        functionExpression: IrFunctionExpression?
    ): IrExpression {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createIrTypeOperator(index: Int): IrTypeOperator {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createIrFunction(
        base: FunctionBaseCarrier,
        modality: Modality,
        isTailrec: Boolean,
        isSuspend: Boolean,
        overridden: List<Int>
    ): IrSimpleFunction {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createIrConstructor(base: FunctionBaseCarrier, isPrimary: Boolean): IrConstructor {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createClassKind(index: Int): ClassKind {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createModalityKind(index: Int): Modality {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createIrTypeParameter(
        base: DeclarationBaseCarrier,
        name: Int,
        index: Int,
        variance: Variance,
        superType: List<Int>,
        isReified: Boolean
    ): IrTypeParameter {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createIrTypeParameterContainer(typeParameter: List<IrTypeParameter>): List<IrTypeParameter> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

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
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createIrTypeAlias(
        base: DeclarationBaseCarrier,
        name: Int,
        visibility: Visibility,
        typeParameters: List<IrTypeParameter>,
        expandedType: Int,
        isActual: Boolean
    ): IrTypeAlias {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createIrEnumEntry(base: DeclarationBaseCarrier, initializer: Int?, correspondingClass: IrClass?, name: Int): IrEnumEntry {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createIrAnonymousInit(base: DeclarationBaseCarrier, body: Int): IrAnonymousInitializer {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createIrDeclaration(
        irAnonymousInit: IrAnonymousInitializer?,
        irClass: IrClass?,
        irConstructor: IrConstructor?,
        irEnumEntry: IrEnumEntry?,
        irField: IrField?,
        irFunction: IrSimpleFunction?,
        irProperty: IrProperty?,
        irTypeParameter: IrTypeParameter?,
        irVariable: IrVariable?,
        irValueParameter: IrValueParameter?,
        irLocalDelegatedProperty: IrLocalDelegatedProperty?,
        irTypeAlias: IrTypeAlias?
    ): IrDeclaration {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createIrBranch(condition: IrExpression, result: IrExpression): IrBranch {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createIrBlockBody(statement: List<IrStatement>): IrBlockBody {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createIrCatch(catchParameter: IrVariable, result: IrExpression): IrCatch {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createIrSyntheticBodyKind(index: Int): IrSyntheticBodyKind {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createIrSyntheticBody(kind: IrSyntheticBodyKind): IrSyntheticBodyKind {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createIrStatement(
        coordinates: CoordinatesCarrier,
        declaration: IrDeclaration?,
        expression: IrExpression?,
        blockBody: IrBlockBody?,
        branch: IrBranch?,
        catch: IrCatch?,
        syntheticBody: IrSyntheticBodyKind?
    ): IrStatement {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}