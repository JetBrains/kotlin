/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.nextgen

import org.jetbrains.kotlin.backend.common.ir.DeclarationFactory
import org.jetbrains.kotlin.backend.common.serialization.DescriptorReferenceDeserializer
import org.jetbrains.kotlin.backend.common.serialization.UniqId
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.SourceManager
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.*
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.IrTypeParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.*
import org.jetbrains.kotlin.ir.util.NaiveSourceBasedFileEntryImpl
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedTypeParameterDescriptor
import org.jetbrains.kotlin.types.Variance

abstract class SmartIrProtoReaderImpl(val symbolTable: SymbolTable, val irBuiltIns: IrBuiltIns, byteArray: ByteArray) : AbstractIrSmartProtoReader(byteArray) {
    override fun createIrTypeArgument_type_(oneOfType: IrTypeProjection) = oneOfType

    override fun createIrDoWhile(
        loopLoopId: Int,
        loopCondition: IrExpression,
        loopLabel: Int?,
        loopOrigin: IrStatementOrigin?
    ): IrDoWhileLoop {
        val loop = IrDoWhileLoopImpl(delayedStart(), delayedEnd(), delayedType(), loopOrigin).apply {
            this.condition = loopCondition
            this.label = loopLabel?.let { loadString(it) }
        }
        registerLoopById(loopLoopId, loop)
        return loop
    }

    override fun createIrDoWhile1(partial: IrDoWhileLoop, loopBody: IrExpression?): IrDoWhileLoop {
        return partial.apply {
            this.body = loopBody
        }
    }

    override fun createIrWhile(loopLoopId: Int, loopCondition: IrExpression, loopLabel: Int?, loopOrigin: IrStatementOrigin?): IrWhileLoop {
        val loop = IrWhileLoopImpl(delayedStart(), delayedEnd(), delayedType(), loopOrigin).apply {
            this.condition = loopCondition
            this.label = loopLabel?.let { loadString(it) }
        }
        registerLoopById(loopLoopId, loop)
        return loop
    }

    override fun createIrWhile1(partial: IrWhileLoop, loopBody: IrExpression?): IrWhileLoop {
        return partial.apply {
            this.body = loopBody
        }
    }

    override fun createIrExpression(
        operation: IrExpression,
        type_: Int,
        coordinatesStartOffset: Int,
        coordinatesEndOffset: Int
    ): IrExpression {
        return operation
    }

    override fun createIrStatement_declaration(
        coordinatesStartOffset: Int,
        coordinatesEndOffset: Int,
        oneOfDeclaration: IrDeclaration
    ): IrElement {
        return oneOfDeclaration
    }

    override fun createIrStatement_expression(
        coordinatesStartOffset: Int,
        coordinatesEndOffset: Int,
        oneOfExpression: IrExpression
    ): IrElement {
        return oneOfExpression
    }

    override fun createIrStatement_blockBody(
        coordinatesStartOffset: Int,
        coordinatesEndOffset: Int,
        oneOfBlockBody: IrBlockBody
    ): IrElement {
        return oneOfBlockBody
    }

    override fun createIrStatement_branch(coordinatesStartOffset: Int, coordinatesEndOffset: Int, oneOfBranch: IrBranch): IrElement {
        return oneOfBranch
    }

    override fun createIrStatement_catch(coordinatesStartOffset: Int, coordinatesEndOffset: Int, oneOfCatch: IrCatch): IrElement {
        return oneOfCatch
    }

    override fun createIrStatement_syntheticBody(
        coordinatesStartOffset: Int,
        coordinatesEndOffset: Int,
        oneOfSyntheticBody: IrSyntheticBody
    ): IrElement {
        return oneOfSyntheticBody
    }

    override fun createIrOperation_block(oneOfBlock: IrBlock) = oneOfBlock

    override fun createIrOperation_break_(oneOfBreak: IrBreak): IrBreak = oneOfBreak

    override fun createIrOperation_call(oneOfCall: IrCall) = oneOfCall

    override fun createIrOperation_classReference(oneOfClassReference: IrClassReference) = oneOfClassReference

    override fun createIrOperation_composite(oneOfComposite: IrComposite) = oneOfComposite

    override fun createIrOperation_const(oneOfConst: IrConst<*>) = oneOfConst

    override fun createIrOperation_continue_(oneOfContinue: IrContinue) = oneOfContinue

    override fun createIrOperation_delegatingConstructorCall(oneOfDelegatingConstructorCall: IrDelegatingConstructorCall) = oneOfDelegatingConstructorCall

    override fun createIrOperation_doWhile(oneOfDoWhile: IrDoWhileLoop) = oneOfDoWhile

    override fun createIrOperation_enumConstructorCall(oneOfEnumConstructorCall: IrEnumConstructorCall) = oneOfEnumConstructorCall

    override fun createIrOperation_functionReference(oneOfFunctionReference: IrFunctionReference) = oneOfFunctionReference

    override fun createIrOperation_getClass(oneOfGetClass: IrGetClass) = oneOfGetClass

    override fun createIrOperation_getEnumValue(oneOfGetEnumValue: IrGetEnumValue) = oneOfGetEnumValue
    override fun createIrOperation_getField(oneOfGetField: IrGetField) = oneOfGetField
    override fun createIrOperation_getObject(oneOfGetObject: IrGetObjectValue) = oneOfGetObject
    override fun createIrOperation_getValue(oneOfGetValue: IrGetValue) = oneOfGetValue
    override fun createIrOperation_instanceInitializerCall(oneOfInstanceInitializerCall: IrInstanceInitializerCall) = oneOfInstanceInitializerCall
    override fun createIrOperation_propertyReference(oneOfpropertyReference: IrPropertyReference) = oneOfpropertyReference
    override fun createIrOperation_return_(oneOfreturn_: IrReturn) = oneOfreturn_
    override fun createIrOperation_setField(oneOfsetField: IrSetField) = oneOfsetField
    override fun createIrOperation_setVariable(oneOfsetVariable: IrSetVariable) = oneOfsetVariable
    override fun createIrOperation_stringConcat(oneOfstringConcat: IrStringConcatenation) = oneOfstringConcat
    override fun createIrOperation_throw_(oneOfthrow_: IrThrow) = oneOfthrow_
    override fun createIrOperation_try_(oneOftry_: IrTry) = oneOftry_
    override fun createIrOperation_typeOp(oneOftypeOp: IrTypeOperatorCall) = oneOftypeOp
    override fun createIrOperation_vararg(oneOfvararg: IrVararg) = oneOfvararg
    override fun createIrOperation_when_(oneOfwhen_: IrWhen) = oneOfwhen_
    override fun createIrOperation_while_(oneOfwhile_: IrWhileLoop) = oneOfwhile_
    override fun createIrOperation_dynamicMember(oneOfdynamicMember: IrDynamicMemberExpression) = oneOfdynamicMember
    override fun createIrOperation_dynamicOperator(oneOfdynamicOperator: IrDynamicOperatorExpression) = oneOfdynamicOperator
    override fun createIrOperation_localDelegatedPropertyReference(oneOflocalDelegatedPropertyReference: IrLocalDelegatedPropertyReference) = oneOflocalDelegatedPropertyReference
    override fun createIrOperation_constructorCall(oneOfconstructorCall: IrConstructorCall) = oneOfconstructorCall
    override fun createIrOperation_functionExpression(oneOffunctionExpression: IrFunctionExpression) = oneOffunctionExpression
    override fun createIrDeclaration_irAnonymousInit(oneOfirAnonymousInit: IrAnonymousInitializer) = oneOfirAnonymousInit
    override fun createIrDeclaration_irClass(oneOfirClass: IrClass) = oneOfirClass
    override fun createIrDeclaration_irConstructor(oneOfirConstructor: IrConstructor) = oneOfirConstructor
    override fun createIrDeclaration_irEnumEntry(oneOfirEnumEntry: IrEnumEntry) = oneOfirEnumEntry
    override fun createIrDeclaration_irField(oneOfirField: IrField) = oneOfirField
    override fun createIrDeclaration_irFunction(oneOfirFunction: IrSimpleFunction) = oneOfirFunction
    override fun createIrDeclaration_irProperty(oneOfirProperty: IrProperty) = oneOfirProperty
    override fun createIrDeclaration_irTypeParameter(oneOfirTypeParameter: IrTypeParameter) = oneOfirTypeParameter
    override fun createIrDeclaration_irVariable(oneOfirVariable: IrVariable) = oneOfirVariable
    override fun createIrDeclaration_irValueParameter(oneOfirValueParameter: IrValueParameter) = oneOfirValueParameter
    override fun createIrDeclaration_irLocalDelegatedProperty(oneOfirLocalDelegatedProperty: IrLocalDelegatedProperty) = oneOfirLocalDelegatedProperty
    override fun createIrDeclaration_irTypeAlias(oneOfirTypeAlias: IrTypeAlias) = oneOfirTypeAlias

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
        uniqId?.let {
            if (isFakeOverride != true) {
                checkBuiltinDescriptor(it)?.let { return it }
            }
        }
        return descriptorReferenceDeserializer.deserializeDescriptorReference(
            packageFqName,
            classFqName,
            loadString(name),
            uniqId?.index,
            isEnumEntry ?: false,
            isEnumSpecial ?: false,
            isDefaultConstructor ?: false,
            isFakeOverride ?: false,
            isGetter ?: false,
            isSetter ?: false,
            isTypeParameter ?: false
        )
    }

    protected abstract fun checkBuiltinDescriptor(id: UniqId): DeclarationDescriptor?

    override fun createIrSymbolData(
        kind: Int,
        uniqId: UniqId,
        topLevelUniqId: UniqId,
        fqname: FqName?,
        descriptorReference: DeclarationDescriptor?
    ) = deserializeIrSymbolData(kind, uniqId, topLevelUniqId, fqname, descriptorReference)

    protected abstract val descriptorReferenceDeserializer: DescriptorReferenceDeserializer

//    protected abstract fun resolveSpecialDescriptor(id: UniqId): DeclarationDescriptor

    protected abstract fun deserializeIrSymbolData(
        kind: Int,
        uniqId: UniqId,
        topLevelUniqId: UniqId,
        fqname: FqName?,
        descriptorReference: DeclarationDescriptor?
    ): IrSymbol

    protected abstract fun loadString(id: Int): String

    protected abstract fun loadType(id: Int): IrType

    protected abstract fun loadSymbol(id: Int): IrSymbol

    protected abstract fun loadStatementBody(id: Int): IrElement

    protected abstract fun loadExpressionBody(id: Int): IrExpression

    private val fileLoops = mutableMapOf<Int, IrLoop>()

    private fun getLoopById(id: Int): IrLoop {
        return fileLoops[id] ?: error("No loop found for id $id")
    }

    private fun registerLoopById(id: Int, loop: IrLoop) {
        fileLoops[id] = loop
    }

    private fun delayedStart(): Int = fieldIrExpressionCoordinatesStartOffset
    private fun delayedEnd(): Int = fieldIrStatementCoordinatesEndOffset
    private fun delayedType(): IrType = fieldIrExpressionType?.let { loadType(it) } ?: irBuiltIns.unitType //error("Should be initialized"))

//    private val moduleDescriptor: ModuleDescriptor get() = TODO("moduleDescriptor")
//    protected abstract val symbolTable: SymbolTable
//    protected abstract val irBuiltInts: IrBuiltIns
//    protected abstract val symbolTable: SymbolTable

    override fun createUniqId(index: Long, isLocal: Boolean) = UniqId(index, isLocal)

    private val visibilities = listOf(
        Visibilities.PUBLIC, Visibilities.PRIVATE, Visibilities.PRIVATE_TO_THIS, Visibilities.PROTECTED, Visibilities
            .INTERNAL, Visibilities.INVISIBLE_FAKE, Visibilities.LOCAL
    )

    override fun createVisibility(name: Int): Visibility {
        val nameString = loadString(name)
        return visibilities.single { it.name == nameString }
    }

    private val allKnownDeclarationOrigins =
        IrDeclarationOrigin::class.nestedClasses.toList() + DeclarationFactory.FIELD_FOR_OUTER_THIS::class

    private val declarationOriginIndex =
        allKnownDeclarationOrigins.map { it.objectInstance as IrDeclarationOriginImpl }.associateBy { it.name }

    private val allKnownStatementOrigins =
        IrStatementOrigin::class.nestedClasses.toList()
    private val statementOriginIndex =
        allKnownStatementOrigins.map { it.objectInstance as? IrStatementOriginImpl }.filterNotNull().associateBy { it.debugName }

    override fun createIrStatementOrigin(name: Int): IrStatementOrigin {
        val nameOrin = loadString(name)
        return nameOrin.let {
            val componentPrefix = "COMPONENT_"
            when {
                it.startsWith(componentPrefix) -> {
                    IrStatementOrigin.COMPONENT_N.withIndex(it.removePrefix(componentPrefix).toInt())
                }
                else -> statementOriginIndex[it] ?: error("Unexpected statement origin: $it")
            }
        }
    }

    override fun createKnownOrigin(index: Int): IrDeclarationOrigin {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createIrDeclarationOrigin_custom(custom: Int): IrDeclarationOrigin {
        val name = loadString(custom)
        return declarationOriginIndex[name] ?: object : IrDeclarationOriginImpl(name) {}
    }

    override fun createIrDeclarationOrigin_origin(origin: IrDeclarationOrigin) = origin

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

        error("Temporary should not be invoked")
//        val packageFragmentDescriptor = EmptyPackageFragmentDescriptor(moduleDescriptor, fqName)

//        val symbol = IrFileSymbolImpl(packageFragmentDescriptor)

        // TODO: Create file deserializer here

//        return IrFileImpl(fileEntry, symbol, fqName)
    }

    override fun createStringTable(strings: List<String>): Array<String> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createIrSymbolKind(index: Int) = index

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

//    override fun createIrTypeArgument_type(type: IrTypeProjection) = type

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

    override fun createIrType_dynamic(oneOfDynamic: IrDynamicType) = oneOfDynamic

    override fun createIrType_error(oneOfError: IrErrorType) = oneOfError

    override fun createIrType_simple(oneOfSimple: IrSimpleType) = oneOfSimple

    override fun createIrTypeTable(types: List<IrType>): Array<IrType> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createIrBreak(loopId: Int, label: Int?): IrBreak {
        return IrBreakImpl(delayedStart(), delayedEnd(), delayedType(), getLoopById(loopId)).also {
            it.label = label?.let { l -> loadString(l) }
        }
    }

    override fun createIrBlock(origin: IrStatementOrigin?, statement: List<IrElement>): IrBlock {
        return IrBlockImpl(delayedStart(), delayedEnd(), delayedType(), origin, statement as List<IrStatement>)
    }

    override fun createIrCall(
        symbol: Int, memberAccessDispatchReceiver: IrExpression?,
        memberAccessExtensionReceiver: IrExpression?,
        memberAccessValueArgument: List<NullableExpression>,
        memberAccessTypeArguments: List<IrType>, super_: Int?, origin: IrStatementOrigin?
    ): IrCall {
        val functionSymbol = loadSymbol(symbol) as IrSimpleFunctionSymbol
        val superSymbol = super_?.let { loadSymbol(it) as IrClassSymbol }
        return IrCallImpl(
            delayedStart(),
            delayedEnd(),
            delayedType(),
            functionSymbol,
            functionSymbol.descriptor,
            memberAccessTypeArguments.size,
            memberAccessValueArgument.size,
            origin, superSymbol
        ).apply {
            dispatchReceiver = memberAccessDispatchReceiver
            extensionReceiver = memberAccessExtensionReceiver
            memberAccessTypeArguments.forEachIndexed { index, type -> putTypeArgument(index, type) }
            memberAccessValueArgument.forEachIndexed { index, value -> putValueArgument(index, value.expression) }
        }
    }

    override fun createIrConstructorCall(
        symbol: Int,
        constructorTypeArgumentsCount: Int,
        memberAccessDispatchReceiver: IrExpression?,
        memberAccessExtensionReceiver: IrExpression?,
        memberAccessValueArgument: List<NullableExpression>,
        memberAccessTypeArguments: List<IrType>
    ): IrConstructorCall {
        val constructorSymbol = loadSymbol(symbol) as IrConstructorSymbol
        return IrConstructorCallImpl(
            delayedStart(),
            delayedEnd(),
            delayedType(),
            constructorSymbol,
            constructorSymbol.descriptor,
            memberAccessTypeArguments.size,
            constructorTypeArgumentsCount,
            memberAccessValueArgument.size
        ).apply {
            dispatchReceiver = memberAccessDispatchReceiver
            extensionReceiver = memberAccessExtensionReceiver
            memberAccessTypeArguments.forEachIndexed { index, type -> putTypeArgument(index, type) }
            memberAccessValueArgument.forEachIndexed { index, value -> putValueArgument(index, value.expression) }
        }
    }

    override fun createIrFunctionReference(
        symbol: Int,
        origin: IrStatementOrigin?,
        memberAccessDispatchReceiver: IrExpression?,
        memberAccessExtensionReceiver: IrExpression?,
        memberAccessValueArgument: List<NullableExpression>,
        memberAccessTypeArguments: List<IrType>
    ): IrFunctionReference {
        val functionSymbol = loadSymbol(symbol) as IrFunctionSymbol
        val callable = IrFunctionReferenceImpl(
            delayedStart(),
            delayedEnd(),
            delayedType(),
            functionSymbol,
            functionSymbol.descriptor,
            memberAccessTypeArguments.size,
            memberAccessValueArgument.size,
            origin
        ).apply {
            dispatchReceiver = memberAccessDispatchReceiver
            extensionReceiver = memberAccessExtensionReceiver
            memberAccessTypeArguments.forEachIndexed { index, type -> putTypeArgument(index, type) }
            memberAccessValueArgument.forEachIndexed { index, value -> putValueArgument(index, value.expression) }
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
        memberAccessDispatchReceiver: IrExpression?,
        memberAccessExtensionReceiver: IrExpression?,
        memberAccessValueArgument: List<NullableExpression>,
        memberAccessTypeArguments: List<IrType>,
        symbol: Int
    ): IrPropertyReference {
        val propertySymbol = loadSymbol(symbol) as IrPropertySymbol

        val fieldSymbol = field?.let { loadSymbol(it) as IrFieldSymbol }
        val getterSymbol = getter?.let { loadSymbol(it) as IrSimpleFunctionSymbol }
        val setterSymbol = setter?.let { loadSymbol(it) as IrSimpleFunctionSymbol }

        return IrPropertyReferenceImpl(
            delayedStart(), delayedEnd(), delayedType(),
            propertySymbol,
            memberAccessTypeArguments.size,
            fieldSymbol,
            getterSymbol,
            setterSymbol,
            origin
        ).apply {
            dispatchReceiver = memberAccessDispatchReceiver
            extensionReceiver = memberAccessExtensionReceiver
            memberAccessTypeArguments.forEachIndexed { index, type -> putTypeArgument(index, type) }
            memberAccessValueArgument.forEachIndexed { index, value -> putValueArgument(index, value.expression) }
        }
    }

    override fun createIrComposite(statement: List<IrElement>, origin: IrStatementOrigin?): IrComposite {
        return IrCompositeImpl(delayedStart(), delayedEnd(), delayedType(), origin, statement as List<IrStatement>)
    }

    override fun createIrClassReference(classSymbol: Int, classType: Int): IrClassReference {
        val symbol = loadSymbol(classSymbol) as IrClassSymbol
        val type = loadType(classType)
        return IrClassReferenceImpl(delayedStart(), delayedEnd(), delayedType(), symbol, type)
    }

    override fun createIrConst_boolean(oneOfBoolean: Boolean) = IrConstImpl.boolean(delayedStart(), delayedEnd(), delayedType(), oneOfBoolean)
    override fun createIrConst_byte(oneOfByte: Int) = IrConstImpl.byte(delayedStart(), delayedEnd(), delayedType(), oneOfByte.toByte())
    override fun createIrConst_char(oneOfChar: Int) = IrConstImpl.char(delayedStart(), delayedEnd(), delayedType(), oneOfChar.toChar())
    override fun createIrConst_double(oneOfDouble: Double) = IrConstImpl.double(delayedStart(), delayedEnd(), delayedType(), oneOfDouble)
    override fun createIrConst_float(oneOfFloat: Float) = IrConstImpl.float(delayedStart(), delayedEnd(), delayedType(), oneOfFloat)
    override fun createIrConst_int(oneOfInt: Int) = IrConstImpl.int(delayedStart(), delayedEnd(), delayedType(), oneOfInt)
    override fun createIrConst_long(oneOfLong: Long) = IrConstImpl.long(delayedStart(), delayedEnd(), delayedType(), oneOfLong)
    override fun createIrConst_null_(oneOfNull: Boolean) = IrConstImpl.constNull(delayedStart(), delayedEnd(), delayedType())
    override fun createIrConst_short(oneOfShort: Int) = IrConstImpl.short(delayedStart(), delayedEnd(), delayedType(), oneOfShort.toShort())
    override fun createIrConst_string(oneOfString: Int) = IrConstImpl.string(delayedStart(), delayedEnd(), delayedType(), loadString(oneOfString))

    override fun createIrContinue(loopId: Int, label: Int?): IrContinue {
        return IrContinueImpl(delayedStart(), delayedEnd(), delayedType(), getLoopById(loopId)).also {
            it.label = label?.let { l -> loadString(l) }
        }
    }

    override fun createIrDelegatingConstructorCall(
        symbol: Int, memberAccessDispatchReceiver: IrExpression?,
        memberAccessExtensionReceiver: IrExpression?,
        memberAccessValueArgument: List<NullableExpression>,
        memberAccessTypeArguments: List<IrType>
    ): IrDelegatingConstructorCall {
        val constructorSymbol = loadSymbol(symbol) as IrConstructorSymbol
        return IrDelegatingConstructorCallImpl(
            delayedStart(),
            delayedEnd(),
            delayedType(),
            constructorSymbol,
            constructorSymbol.descriptor,
            memberAccessTypeArguments.size,
            memberAccessValueArgument.size
        ).apply {
            dispatchReceiver = memberAccessDispatchReceiver
            extensionReceiver = memberAccessExtensionReceiver
            memberAccessTypeArguments.forEachIndexed { index, type -> putTypeArgument(index, type) }
            memberAccessValueArgument.forEachIndexed { index, value -> putValueArgument(index, value.expression) }
        }
    }

    override fun createIrEnumConstructorCall(
        symbol: Int,
        memberAccessDispatchReceiver: IrExpression?,
        memberAccessExtensionReceiver: IrExpression?,
        memberAccessValueArgument: List<NullableExpression>,
        memberAccessTypeArguments: List<IrType>
    ): IrEnumConstructorCall {
        val constructorSymbol = loadSymbol(symbol) as IrConstructorSymbol
        return IrEnumConstructorCallImpl(
            delayedStart(),
            delayedEnd(),
            delayedType(),
            constructorSymbol,
            memberAccessTypeArguments.size,
            memberAccessValueArgument.size
        ).apply {
            dispatchReceiver = memberAccessDispatchReceiver
            extensionReceiver = memberAccessExtensionReceiver
            memberAccessTypeArguments.forEachIndexed { index, type -> putTypeArgument(index, type) }
            memberAccessValueArgument.forEachIndexed { index, value -> putValueArgument(index, value.expression) }
        }
    }

    override fun createIrGetClass(argument: IrExpression): IrGetClass {
        return IrGetClassImpl(delayedStart(), delayedEnd(), delayedType(), argument)
    }

    override fun createIrGetEnumValue(symbol: Int): IrGetEnumValue {
        val enumEntrySymbol = loadSymbol(symbol) as IrEnumEntrySymbol
        return IrGetEnumValueImpl(delayedStart(), delayedEnd(), delayedType(), enumEntrySymbol)
    }

    override fun createIrGetField(
        fieldAccessSymbol: Int,
        fieldAccessSuper: Int?,
        fieldAccessReceiver: IrExpression?,
        origin: IrStatementOrigin?
    ): IrGetField {
        val fieldSymbol = loadSymbol(fieldAccessSymbol) as IrFieldSymbol
        val superSymbol = fieldAccessSuper?.let { loadSymbol(it) as IrClassSymbol }
        return IrGetFieldImpl(delayedStart(), delayedEnd(), fieldSymbol, delayedType(), fieldAccessReceiver, origin, superSymbol)
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

    override fun createIrReturn(returnTarget: Int, value: IrExpression): IrReturn {
        val returnSymbol = loadSymbol(returnTarget) as IrReturnTargetSymbol
        return IrReturnImpl(delayedStart(), delayedEnd(), delayedType(), returnSymbol, value)
    }

    override fun createIrSetField(
        fieldAccessSymbol: Int,
        fieldAccessSuper: Int?,
        fieldAccessReceiver: IrExpression?, value: IrExpression, origin: IrStatementOrigin?
    ): IrSetField {
        val fieldSymbol = loadSymbol(fieldAccessSymbol) as IrFieldSymbol
        val superSymbol = fieldAccessSuper?.let { loadSymbol(it) as IrClassSymbol }
        return IrSetFieldImpl(delayedStart(), delayedEnd(), fieldSymbol, fieldAccessReceiver, value, delayedType(), origin, superSymbol)
    }

    override fun createIrSetVariable(symbol: Int, value: IrExpression, origin: IrStatementOrigin?): IrSetVariable {
        val variableSymbol = loadSymbol(symbol) as IrVariableSymbol
        return IrSetVariableImpl(delayedStart(), delayedEnd(), delayedType(), variableSymbol, value, origin)
    }

    override fun createIrSpreadElement(expression: IrExpression, coordinatesStartOffset: Int, coordinatesEndOffset: Int): IrSpreadElement {
        return IrSpreadElementImpl(coordinatesStartOffset, coordinatesEndOffset, expression)
    }

    override fun createIrStringConcat(argument: List<IrExpression>): IrStringConcatenation {
        return IrStringConcatenationImpl(delayedStart(), delayedEnd(), delayedType(), argument)
    }

    override fun createIrThrow(value: IrExpression): IrThrow {
        return IrThrowImpl(delayedStart(), delayedEnd(), delayedType(), value)
    }

    override fun createIrTry(result: IrExpression, catch: List<IrElement>, finally: IrExpression?): IrTry {
        return IrTryImpl(delayedStart(), delayedEnd(), delayedType(), result, catch as List<IrCatch>, finally)
    }

    override fun createIrTypeOp(operator: IrTypeOperator, operand: Int, argument: IrExpression): IrTypeOperatorCall {
        return IrTypeOperatorCallImpl(delayedStart(), delayedEnd(), delayedType(), operator, loadType(operand), argument)
    }

    override fun createIrVararg(elementType: Int, element: List<IrVarargElement>): IrVararg {
        return IrVarargImpl(delayedStart(), delayedEnd(), delayedType(), loadType(elementType), element)
    }

    override fun createIrVarargElement_expression(oneOfExpression: IrExpression) = oneOfExpression

    override fun createIrVarargElement_spreadElement(oneOfSpreadElement: IrSpreadElement) = oneOfSpreadElement

    override fun createIrWhen(branch: List<IrElement>, origin: IrStatementOrigin?): IrWhen {
        return IrWhenImpl(delayedStart(), delayedEnd(), delayedType(), origin, branch as List<IrBranch>)
    }

//    override fun createIrWhile(loop: LoopCarrier): IrWhileLoop {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }

    override fun createIrFunctionExpression(function: IrSimpleFunction, origin: IrStatementOrigin): IrFunctionExpression {
        return IrFunctionExpressionImpl(delayedStart(), delayedEnd(), delayedType(), function, origin)
    }

    override fun createIrDynamicMemberExpression(memberName: Int, receiver: IrExpression): IrDynamicMemberExpression {
        return IrDynamicMemberExpressionImpl(delayedStart(), delayedEnd(), delayedType(), loadString(memberName), receiver)
    }

    override fun createIrDynamicOperator(index: Int): IrDynamicOperator {
        return IrDynamicOperator.values()[index - 1]
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
        return IrTypeOperator.values()[index - 1]
    }

//    override fun createIrExpression(operation: IrExpression, type: Int, coordinates: CoordinatesCarrier): IrExpression {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }

    override fun createNullableIrExpression(expression: IrExpression?): NullableExpression {
        return NullableExpression(expression)
    }


    override fun createIrFunction(
        baseBaseSymbol: Int,
        baseBaseOrigin: IrDeclarationOrigin,
        baseBaseCoordinatesStartOffset: Int,
        baseBaseCoordinatesEndOffset: Int,
        baseBaseAnnotations: List<IrConstructorCall>,
        baseName: Int,
        baseVisibility: Visibility,
        baseIsInline: Boolean,
        baseIsExternal: Boolean,
        baseReturnType: Int,
        modality: Modality,
        isTailrec: Boolean,
        isSuspend: Boolean,
        overridden: List<Int>
    ): IrSimpleFunction {
        val start = baseBaseCoordinatesStartOffset
        val end = baseBaseCoordinatesEndOffset
        val origin = baseBaseOrigin
        val functionSymbol = loadSymbol(baseBaseSymbol) as IrSimpleFunctionSymbol
        val name = Name.guessByFirstCharacter(loadString(baseName))
        val returnType = loadType(baseReturnType)
        val visibility = baseVisibility
        return IrFunctionImpl(
            start,
            end,
            origin,
            functionSymbol,
            name,
            visibility,
            modality,
            returnType,
            baseIsInline,
            baseIsExternal,
            isTailrec,
            isSuspend
        ).apply {
            annotations.addAll(baseBaseAnnotations)
            overridden.mapTo(overriddenSymbols) { loadSymbol(it) as IrSimpleFunctionSymbol }

//            (descriptor as? WrappedSimpleFunctionDescriptor).bind(this)
        }
    }

    override fun createIrFunction1(
        partial: IrSimpleFunction,
        baseTypeParameters: List<IrTypeParameter>,
        baseDispatchReceiver: IrValueParameter?,
        baseExtensionReceiver: IrValueParameter?,
        baseValueParameter: List<IrValueParameter>,
        baseBody: Int?
    ): IrSimpleFunction {
        return partial.apply {
            dispatchReceiverParameter = baseDispatchReceiver
            extensionReceiverParameter = baseExtensionReceiver
            valueParameters.addAll(baseValueParameter)
            typeParameters.addAll(baseTypeParameters)

            body = baseBody?.let { loadStatementBody(it) as IrBody }
        }
    }

    override fun createIrConstructor(
        baseBaseSymbol: Int,
        baseBaseOrigin: IrDeclarationOrigin,
        baseBaseCoordinatesStartOffset: Int,
        baseBaseCoordinatesEndOffset: Int,
        baseBaseAnnotations: List<IrConstructorCall>,
        baseName: Int,
        baseVisibility: Visibility,
        baseIsInline: Boolean,
        baseIsExternal: Boolean,
        baseReturnType: Int,
        isPrimary: Boolean
    ): IrConstructor {
        val start = baseBaseCoordinatesStartOffset
        val end = baseBaseCoordinatesEndOffset
        val origin = baseBaseOrigin
        val constructorSymbol = loadSymbol(baseBaseSymbol) as IrConstructorSymbol
        val name = Name.special(loadString(baseName))
        val returnType = loadType(baseReturnType)
        val visibility = baseVisibility
        return IrConstructorImpl(
            start,
            end,
            origin,
            constructorSymbol,
            name,
            visibility,
            returnType,
            baseIsInline,
            baseIsExternal,
            isPrimary
        ).apply {
            annotations.addAll(baseBaseAnnotations)

            // (descriptor as? WrappedClassConstructorDescriptor)?.bind(this)
        }
    }

    override fun createIrConstructor1(
        partial: IrConstructor,
        baseTypeParameters: List<IrTypeParameter>,
        baseDispatchReceiver: IrValueParameter?,
        baseExtensionReceiver: IrValueParameter?,
        baseValueParameter: List<IrValueParameter>,
        baseBody: Int?
    ): IrConstructor {
        val bodyIndex = baseBody
        return partial.apply {
            dispatchReceiverParameter = baseDispatchReceiver
            extensionReceiverParameter = baseExtensionReceiver
            valueParameters.addAll(baseValueParameter)
            typeParameters.addAll(baseTypeParameters)

            body = bodyIndex?.let { loadStatementBody(it) as IrBody }
        }
    }


    override fun createIrField(
        baseSymbol: Int,
        baseOrigin: IrDeclarationOrigin,
        baseCoordinatesStartOffset: Int,
        baseCoordinatesEndOffset: Int,
        baseAnnotations: List<IrConstructorCall>,
        initializer: Int?,
        name: Int,
        visibility: Visibility,
        isFinal: Boolean,
        isExternal: Boolean,
        isStatic: Boolean,
        type_: Int
    ): IrField {
        val start = baseCoordinatesStartOffset
        val end = baseCoordinatesEndOffset
        val origin = baseOrigin
        val fieldSymbol = loadSymbol(baseSymbol) as IrFieldSymbol
        val fieldName = Name.guessByFirstCharacter(loadString(name))
        val fieldType = loadType(type_)
        return IrFieldImpl(start, end, origin, fieldSymbol, fieldName, fieldType, visibility, isFinal, isExternal, isStatic).apply {
            this.annotations.addAll(baseAnnotations)
            this.initializer = initializer?.let { IrExpressionBodyImpl(loadExpressionBody(it)) }

//            (descriptor as? WrappedFieldDescriptor)?.bind(this)
        }
    }

    override fun createIrLocalDelegatedProperty(
        baseSymbol: Int,
        baseOrigin: IrDeclarationOrigin,
        baseCoordinatesStartOffset: Int,
        baseCoordinatesEndOffset: Int,
        baseAnnotations: List<IrConstructorCall>,
        name: Int,
        type_: Int,
        isVar: Boolean,
        delegate: IrVariable,
        getter: IrSimpleFunction?,
        setter: IrSimpleFunction?
    ): IrLocalDelegatedProperty {
        val start = baseCoordinatesStartOffset
        val end = baseCoordinatesEndOffset
        val origin = baseOrigin
        val propertySymbol = loadSymbol(baseSymbol) as IrLocalDelegatedPropertySymbol
        val propertyType = loadType(type_)
        val propertyName = Name.guessByFirstCharacter(loadString(name))

        return IrLocalDelegatedPropertyImpl(start, end, origin, propertySymbol, propertyName, propertyType, isVar).apply {
            this.annotations.addAll(baseAnnotations)
            this.delegate = delegate
            this.getter = getter ?: error("Should be non-null for some reason")
            this.setter = setter

//            (descriptor as? WrappedVariableDescriptorWithAccessor)?.bind(this)
        }
    }

    override fun createIrProperty(
        baseSymbol: Int,
        baseOrigin: IrDeclarationOrigin,
        baseCoordinatesStartOffset: Int,
        baseCoordinatesEndOffset: Int,
        baseAnnotations: List<IrConstructorCall>,
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
        val start = baseCoordinatesStartOffset
        val end = baseCoordinatesEndOffset
        val origin = baseOrigin
        val propertySymbol = loadSymbol(baseSymbol) as IrPropertySymbol
        val propertyName = Name.guessByFirstCharacter(loadString(name))

        return IrPropertyImpl(start, end, origin, propertySymbol, propertyName, visibility, modality, isVar, isConst, isLateinit, isDelegated, isExternal).apply {
            this.annotations.addAll(baseAnnotations)
            this.backingField = backingField?.also { it.correspondingPropertySymbol = propertySymbol }
            this.getter = getter?.also { it.correspondingPropertySymbol = propertySymbol }
            this.setter = setter?.also { it.correspondingPropertySymbol = propertySymbol }

//            (descriptor as? WrappedPropertyDescriptor)?.bind(this)
        }
    }

    override fun createIrVariable(
        baseSymbol: Int,
        baseOrigin: IrDeclarationOrigin,
        baseCoordinatesStartOffset: Int,
        baseCoordinatesEndOffset: Int,
        baseAnnotations: List<IrConstructorCall>,
        name: Int,
        type_: Int,
        isVar: Boolean,
        isConst: Boolean,
        isLateinit: Boolean,
        initializer: IrExpression?
    ): IrVariable {
        val start = baseCoordinatesStartOffset
        val end = baseCoordinatesEndOffset
        val origin = baseOrigin
        val variableSymbol = loadSymbol(baseSymbol) as IrVariableSymbol
        val variableName = Name.guessByFirstCharacter(loadString(name))

        return IrVariableImpl(start, end, origin, variableSymbol, variableName, loadType(type_), isVar, isConst, isLateinit).apply {
            this.annotations.addAll(baseAnnotations)
            this.initializer = initializer
        }
    }

    override fun createClassKind(index: Int): ClassKind {
        return ClassKind.values()[index - 1]
    }

    override fun createModalityKind(index: Int): Modality {
        return Modality.values()[index - 1]
    }

    override fun createIrValueParameter(
        baseSymbol: Int,
        baseOrigin: IrDeclarationOrigin,
        baseCoordinatesStartOffset: Int,
        baseCoordinatesEndOffset: Int,
        baseAnnotations: List<IrConstructorCall>,
        name: Int,
        index: Int,
        type_: Int,
        varargElementType: Int?,
        isCrossinline: Boolean,
        isNoinline: Boolean,
        defaultValue: Int?
    ): IrValueParameter {
        val start = baseCoordinatesStartOffset
        val end = baseCoordinatesEndOffset
        val origin = baseOrigin
        val valueSymbol = loadSymbol(baseSymbol) as IrValueParameterSymbol
        val valueName = Name.guessByFirstCharacter(loadString(name))
        val valueType = loadType(type_)
        val valueVarargType = varargElementType?.let { loadType(it) }
        return IrValueParameterImpl(start, end, origin, valueSymbol, valueName, index, valueType, valueVarargType, isCrossinline, isNoinline).apply {
            this.annotations.addAll(baseAnnotations)
            this.defaultValue = defaultValue?.let { IrExpressionBodyImpl(loadExpressionBody(it)) }

//            (descriptor as? WrappedValueParameterDescriptor)?.bind(this)
//            (descriptor as? WrappedReceiverParameterDescriptor)?.bind(this)
        }

    }

    override fun createIrTypeParameter(
        baseSymbol: Int,
        baseOrigin: IrDeclarationOrigin,
        baseCoordinatesStartOffset: Int,
        baseCoordinatesEndOffset: Int,
        baseAnnotations: List<IrConstructorCall>,
        name: Int,
        index: Int,
        variance: Variance,
        superType: List<Int>,
        isReified: Boolean
    ): IrTypeParameter {
        val start = baseCoordinatesStartOffset
        val end = baseCoordinatesEndOffset
        val origin = baseOrigin
        val typeParameterSymbol = loadSymbol(baseSymbol) as IrTypeParameterSymbol
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
            this.annotations.addAll(baseAnnotations)
            superType.mapTo(this.superTypes) { loadType(it) }

//            (typeParameterDescriptor as? WrappedTypeParameterDescriptor)?.bind(this)
        }
    }

    override fun createIrTypeParameterContainer(typeParameter: List<IrTypeParameter>) = typeParameter


    override fun createIrClass(
        baseSymbol: Int,
        baseOrigin: IrDeclarationOrigin,
        baseCoordinatesStartOffset: Int,
        baseCoordinatesEndOffset: Int,
        baseAnnotations: List<IrConstructorCall>,
        name: Int,
        kind: ClassKind,
        visibility: Visibility,
        modality: Modality,
        isCompanion: Boolean,
        isInner: Boolean,
        isData: Boolean,
        isExternal: Boolean,
        isInline: Boolean,
        superType: List<Int>
    ): IrClass {
        val start = baseCoordinatesStartOffset
        val end = baseCoordinatesEndOffset
        val origin = baseOrigin
        val classSymbol = loadSymbol(baseSymbol) as IrClassSymbol
        val className = Name.guessByFirstCharacter(loadString(name))

        return IrClassImpl(
            start,
            end,
            origin,
            classSymbol,
            className,
            kind,
            visibility,
            modality,
            isCompanion,
            isInner,
            isData,
            isExternal,
            isInline
        ).apply {
            this.annotations.addAll(baseAnnotations)
            superType.mapTo(this.superTypes) { loadType(it) }

//            (descriptor as? WrappedClassDescriptor)?.bind(this)
        }
    }

    override fun createIrClass1(partial: IrClass, thisReceiver: IrValueParameter?, typeParameters: List<IrTypeParameter>): IrClass {
        return partial.apply {
            this.thisReceiver = thisReceiver?.also { it.parent = partial }
            typeParameters.forEach { it.parent = partial }
            this.typeParameters.addAll(typeParameters)
        }
    }

    override fun createIrClass2(partial: IrClass, declarationContainer: List<IrDeclaration>): IrClass {
        return partial.apply {
            declarationContainer.forEach { it.parent = partial }
            this.declarations.addAll(declarationContainer)
        }
    }

    override fun createIrTypeAlias(
        baseSymbol: Int,
        baseOrigin: IrDeclarationOrigin,
        baseCoordinatesStartOffset: Int,
        baseCoordinatesEndOffset: Int,
        baseAnnotations: List<IrConstructorCall>,
        name: Int,
        visibility: Visibility,
        typeParameters: List<IrTypeParameter>,
        expandedType: Int,
        isActual: Boolean
    ): IrTypeAlias {
        val start = baseCoordinatesStartOffset
        val end = baseCoordinatesEndOffset
        val origin = baseOrigin
        val typeAliasSymbol = loadSymbol(baseSymbol) as IrTypeAliasSymbol
        val typeAliasName = Name.guessByFirstCharacter(loadString(name))

        return IrTypeAliasImpl(start, end, typeAliasSymbol, typeAliasName, visibility, loadType(expandedType), isActual, origin).apply {
            this.annotations.addAll(baseAnnotations)
            this.typeParameters.addAll(typeParameters)

//            (descriptor as? WrappedTypeAliasDescriptor)?.bind(this)
        }
    }

    override fun createIrEnumEntry(
        baseSymbol: Int,
        baseOrigin: IrDeclarationOrigin,
        baseCoordinatesStartOffset: Int,
        baseCoordinatesEndOffset: Int,
        baseAnnotations: List<IrConstructorCall>, initializer: Int?, correspondingClass: IrClass?, name: Int
    ): IrEnumEntry {
        val start = baseCoordinatesStartOffset
        val end = baseCoordinatesEndOffset
        val origin = baseOrigin
        val enumEntrySymbol = loadSymbol(baseSymbol) as IrEnumEntrySymbol
        val enumEntryName = Name.guessByFirstCharacter(loadString(name))

        return IrEnumEntryImpl(start, end, origin, enumEntrySymbol, enumEntryName).apply {
            this.annotations.addAll(baseAnnotations)
            this.correspondingClass = correspondingClass
            this.initializerExpression = initializer?.let { loadExpressionBody(it) }

//            (descriptor as? WrappedEnumEntryDescriptor)?.bind(this)
        }
    }

    override fun createIrAnonymousInit(
        baseSymbol: Int,
        baseOrigin: IrDeclarationOrigin,
        baseCoordinatesStartOffset: Int,
        baseCoordinatesEndOffset: Int,
        baseAnnotations: List<IrConstructorCall>,
        body: Int
    ): IrAnonymousInitializer {
        val start = baseCoordinatesStartOffset
        val end = baseCoordinatesEndOffset
        val origin = baseOrigin
        val initSymbol = loadSymbol(baseSymbol) as IrAnonymousInitializerSymbol

        return IrAnonymousInitializerImpl(start, end, origin, initSymbol).apply {
            this.annotations.addAll(baseAnnotations)
            this.body = loadStatementBody(body) as IrBlockBody
        }
    }

    override fun createIrBranch(condition: IrExpression, result: IrExpression): IrBranch {
        return IrBranchImpl(fieldIrStatementCoordinatesStartOffset, fieldIrStatementCoordinatesEndOffset, condition, result)
    }

    override fun createIrBlockBody(statement: List<IrElement>): IrBlockBody {
        return IrBlockBodyImpl(fieldIrStatementCoordinatesStartOffset, fieldIrStatementCoordinatesEndOffset, statement as List<IrStatement>)
    }

    override fun createIrCatch(catchParameter: IrVariable, result: IrExpression): IrCatch {
        return IrCatchImpl(fieldIrStatementCoordinatesStartOffset, fieldIrStatementCoordinatesEndOffset, catchParameter, result)
    }

    override fun createIrSyntheticBodyKind(index: Int): IrSyntheticBodyKind {
        return IrSyntheticBodyKind.values()[index - 1]
    }

    override fun createIrSyntheticBody(kind: IrSyntheticBodyKind): IrSyntheticBody {
        return IrSyntheticBodyImpl(fieldIrStatementCoordinatesStartOffset, fieldIrStatementCoordinatesEndOffset, kind)
    }
}