package org.jetbrains.kotlin.backend.common.serialization.protoMimic

class DescriptorReference(
    val packageFqName: FqName,
    val classFqName: FqName,
    val name: IrDataIndex,
    private val fieldUniqId: UniqId?,
    val isGetter: Boolean,
    val isSetter: Boolean,
    val isBackingField: Boolean,
    val isFakeOverride: Boolean,
    val isDefaultConstructor: Boolean,
    val isEnumEntry: Boolean,
    val isEnumSpecial: Boolean,
    val isTypeParameter: Boolean
) {

    val uniqId: UniqId
        get() = fieldUniqId!!

    fun hasPackageFqName(): Boolean = true

    fun hasClassFqName(): Boolean = true

    fun hasName(): Boolean = true

    fun hasUniqId(): Boolean = fieldUniqId != null

    fun hasIsGetter(): Boolean = true

    fun hasIsSetter(): Boolean = true

    fun hasIsBackingField(): Boolean = true

    fun hasIsFakeOverride(): Boolean = true

    fun hasIsDefaultConstructor(): Boolean = true

    fun hasIsEnumEntry(): Boolean = true

    fun hasIsEnumSpecial(): Boolean = true

    fun hasIsTypeParameter(): Boolean = true
}

class UniqId(
    val index: Long,
    val isLocal: Boolean
) {

    fun hasIndex(): Boolean = true

    fun hasIsLocal(): Boolean = true
}

class Coordinates(
    val startOffset: Int,
    val endOffset: Int
) {

    fun hasStartOffset(): Boolean = true

    fun hasEndOffset(): Boolean = true
}

class Visibility(
    val name: IrDataIndex
) {

    fun hasName(): Boolean = true
}

class IrStatementOrigin(
    val name: IrDataIndex
) {

    fun hasName(): Boolean = true
}

enum class KnownOrigin {
    CUSTOM,
    DEFINED,
    FAKE_OVERRIDE,
    FOR_LOOP_ITERATOR,
    FOR_LOOP_VARIABLE,
    FOR_LOOP_IMPLICIT_VARIABLE,
    PROPERTY_BACKING_FIELD,
    DEFAULT_PROPERTY_ACCESSOR,
    DELEGATE,
    DELEGATED_PROPERTY_ACCESSOR,
    DELEGATED_MEMBER,
    ENUM_CLASS_SPECIAL_MEMBER,
    FUNCTION_FOR_DEFAULT_PARAMETER,
    FILE_CLASS,
    GENERATED_DATA_CLASS_MEMBER,
    GENERATED_INLINE_CLASS_MEMBER,
    LOCAL_FUNCTION_FOR_LAMBDA,
    CATCH_PARAMETER,
    INSTANCE_RECEIVER,
    PRIMARY_CONSTRUCTOR_PARAMETER,
    IR_TEMPORARY_VARIABLE,
    IR_EXTERNAL_DECLARATION_STUB,
    IR_EXTERNAL_JAVA_DECLARATION_STUB,
    IR_BUILTINS_STUB,
    BRIDGE,
    FIELD_FOR_ENUM_ENTRY,
    FIELD_FOR_ENUM_VALUES,
    FIELD_FOR_OBJECT_INSTANCE;

    companion object {
        fun fromIndex(index: Int): KnownOrigin {
            return when (index) {
                1 -> CUSTOM
                2 -> DEFINED
                3 -> FAKE_OVERRIDE
                4 -> FOR_LOOP_ITERATOR
                5 -> FOR_LOOP_VARIABLE
                6 -> FOR_LOOP_IMPLICIT_VARIABLE
                7 -> PROPERTY_BACKING_FIELD
                8 -> DEFAULT_PROPERTY_ACCESSOR
                9 -> DELEGATE
                10 -> DELEGATED_PROPERTY_ACCESSOR
                11 -> DELEGATED_MEMBER
                12 -> ENUM_CLASS_SPECIAL_MEMBER
                13 -> FUNCTION_FOR_DEFAULT_PARAMETER
                14 -> FILE_CLASS
                15 -> GENERATED_DATA_CLASS_MEMBER
                16 -> GENERATED_INLINE_CLASS_MEMBER
                17 -> LOCAL_FUNCTION_FOR_LAMBDA
                19 -> CATCH_PARAMETER
                20 -> INSTANCE_RECEIVER
                21 -> PRIMARY_CONSTRUCTOR_PARAMETER
                22 -> IR_TEMPORARY_VARIABLE
                23 -> IR_EXTERNAL_DECLARATION_STUB
                24 -> IR_EXTERNAL_JAVA_DECLARATION_STUB
                25 -> IR_BUILTINS_STUB
                26 -> BRIDGE
                27 -> FIELD_FOR_ENUM_ENTRY
                28 -> FIELD_FOR_ENUM_VALUES
                29 -> FIELD_FOR_OBJECT_INSTANCE
                else -> error("Unexpected enum value '$index' for enum 'KnownOrigin'")
            }
        }
    }
}

class IrDeclarationOrigin(
    val eitherCase: EitherCase,
    private val fieldOrigin: KnownOrigin?,
    private val fieldCustom: IrDataIndex?
) {
    enum class EitherCase {
        ORIGIN,
        CUSTOM,
        EITHER_NOT_SET
    }

    val origin: KnownOrigin
        get() = fieldOrigin!!

    val custom: IrDataIndex
        get() = fieldCustom!!

    fun hasOrigin(): Boolean = fieldOrigin != null

    fun hasCustom(): Boolean = fieldCustom != null
}

class IrDataIndex(
    val index: Int
) {

    fun hasIndex(): Boolean = true
}

class FqName(
    val segmentList: List<IrDataIndex>
) {

    fun hasSegment(): Boolean = true

    val segmentCount: Int
        get() = segmentList.size
}

class IrDeclarationContainer(
    val declarationList: List<IrDeclaration>
) {

    fun hasDeclaration(): Boolean = true

    val declarationCount: Int
        get() = declarationList.size
}

class FileEntry(
    val name: String,
    val lineStartOffsetsList: List<Int>
) {

    fun hasName(): Boolean = true

    fun hasLineStartOffsets(): Boolean = true

    val lineStartOffsetsCount: Int
        get() = lineStartOffsetsList.size
}

class IrFile(
    val declarationIdList: List<UniqId>,
    val fileEntry: FileEntry,
    val fqName: FqName,
    val annotations: Annotations,
    val explicitlyExportedToCompilerList: List<IrDataIndex>
) {

    fun hasDeclarationId(): Boolean = true

    val declarationIdCount: Int
        get() = declarationIdList.size

    fun hasFileEntry(): Boolean = true

    fun hasFqName(): Boolean = true

    fun hasAnnotations(): Boolean = true

    fun hasExplicitlyExportedToCompiler(): Boolean = true

    val explicitlyExportedToCompilerCount: Int
        get() = explicitlyExportedToCompilerList.size
}

class StringTable(
    val stringsList: List<String>
) {

    fun hasStrings(): Boolean = true

    val stringsCount: Int
        get() = stringsList.size
}

enum class IrSymbolKind {
    FUNCTION_SYMBOL,
    CONSTRUCTOR_SYMBOL,
    ENUM_ENTRY_SYMBOL,
    FIELD_SYMBOL,
    VALUE_PARAMETER_SYMBOL,
    RETURNABLE_BLOCK_SYMBOL,
    CLASS_SYMBOL,
    TYPE_PARAMETER_SYMBOL,
    VARIABLE_SYMBOL,
    ANONYMOUS_INIT_SYMBOL,
    STANDALONE_FIELD_SYMBOL,
    RECEIVER_PARAMETER_SYMBOL,
    PROPERTY_SYMBOL,
    LOCAL_DELEGATED_PROPERTY_SYMBOL,
    TYPEALIAS_SYMBOL;

    companion object {
        fun fromIndex(index: Int): IrSymbolKind {
            return when (index) {
                1 -> FUNCTION_SYMBOL
                2 -> CONSTRUCTOR_SYMBOL
                3 -> ENUM_ENTRY_SYMBOL
                4 -> FIELD_SYMBOL
                5 -> VALUE_PARAMETER_SYMBOL
                6 -> RETURNABLE_BLOCK_SYMBOL
                7 -> CLASS_SYMBOL
                8 -> TYPE_PARAMETER_SYMBOL
                9 -> VARIABLE_SYMBOL
                10 -> ANONYMOUS_INIT_SYMBOL
                11 -> STANDALONE_FIELD_SYMBOL
                12 -> RECEIVER_PARAMETER_SYMBOL
                13 -> PROPERTY_SYMBOL
                14 -> LOCAL_DELEGATED_PROPERTY_SYMBOL
                15 -> TYPEALIAS_SYMBOL
                else -> error("Unexpected enum value '$index' for enum 'IrSymbolKind'")
            }
        }
    }
}

class IrSymbolData(
    val kind: IrSymbolKind,
    val uniqId: UniqId,
    val topLevelUniqId: UniqId,
    private val fieldFqname: FqName?,
    private val fieldDescriptorReference: DescriptorReference?
) {

    val fqname: FqName
        get() = fieldFqname!!

    val descriptorReference: DescriptorReference
        get() = fieldDescriptorReference!!

    fun hasKind(): Boolean = true

    fun hasUniqId(): Boolean = true

    fun hasTopLevelUniqId(): Boolean = true

    fun hasFqname(): Boolean = fieldFqname != null

    fun hasDescriptorReference(): Boolean = fieldDescriptorReference != null
}

class IrSymbolTable(
    val symbolsList: List<IrSymbolData>
) {

    fun hasSymbols(): Boolean = true

    val symbolsCount: Int
        get() = symbolsList.size
}

enum class IrTypeVariance {
    IN,
    OUT,
    INV;

    companion object {
        fun fromIndex(index: Int): IrTypeVariance {
            return when (index) {
                0 -> IN
                1 -> OUT
                2 -> INV
                else -> error("Unexpected enum value '$index' for enum 'IrTypeVariance'")
            }
        }
    }
}

class Annotations(
    val annotationList: List<IrConstructorCall>
) {

    fun hasAnnotation(): Boolean = true

    val annotationCount: Int
        get() = annotationList.size
}

class TypeArguments(
    val typeArgumentList: List<IrDataIndex>
) {

    fun hasTypeArgument(): Boolean = true

    val typeArgumentCount: Int
        get() = typeArgumentList.size
}

class IrStarProjection(
    private val fieldVoid: Boolean?
) {

    val void: Boolean
        get() = fieldVoid!!

    fun hasVoid(): Boolean = fieldVoid != null
}

class IrTypeProjection(
    val variance: IrTypeVariance,
    val type: IrDataIndex
) {

    fun hasVariance(): Boolean = true

    fun hasType(): Boolean = true
}

class IrTypeArgument(
    val kindCase: KindCase,
    private val fieldStar: IrStarProjection?,
    private val fieldType: IrTypeProjection?
) {
    enum class KindCase {
        STAR,
        TYPE,
        KIND_NOT_SET
    }

    val star: IrStarProjection
        get() = fieldStar!!

    val type: IrTypeProjection
        get() = fieldType!!

    fun hasStar(): Boolean = fieldStar != null

    fun hasType(): Boolean = fieldType != null
}

class IrSimpleType(
    val annotations: Annotations,
    val classifier: IrDataIndex,
    val hasQuestionMark: Boolean,
    val argumentList: List<IrTypeArgument>,
    private val fieldAbbreviation: IrTypeAbbreviation?
) {

    val abbreviation: IrTypeAbbreviation
        get() = fieldAbbreviation!!

    fun hasAnnotations(): Boolean = true

    fun hasClassifier(): Boolean = true

    fun hasHasQuestionMark(): Boolean = true

    fun hasArgument(): Boolean = true

    val argumentCount: Int
        get() = argumentList.size

    fun hasAbbreviation(): Boolean = fieldAbbreviation != null
}

class IrTypeAbbreviation(
    val annotations: Annotations,
    val typeAlias: IrDataIndex,
    val hasQuestionMark: Boolean,
    val argumentList: List<IrTypeArgument>
) {

    fun hasAnnotations(): Boolean = true

    fun hasTypeAlias(): Boolean = true

    fun hasHasQuestionMark(): Boolean = true

    fun hasArgument(): Boolean = true

    val argumentCount: Int
        get() = argumentList.size
}

class IrDynamicType(
    val annotations: Annotations
) {

    fun hasAnnotations(): Boolean = true
}

class IrErrorType(
    val annotations: Annotations
) {

    fun hasAnnotations(): Boolean = true
}

class IrType(
    val kindCase: KindCase,
    private val fieldSimple: IrSimpleType?,
    private val fieldDynamic: IrDynamicType?,
    private val fieldError: IrErrorType?
) {
    enum class KindCase {
        SIMPLE,
        DYNAMIC,
        ERROR,
        KIND_NOT_SET
    }

    val simple: IrSimpleType
        get() = fieldSimple!!

    val dynamic: IrDynamicType
        get() = fieldDynamic!!

    val error: IrErrorType
        get() = fieldError!!

    fun hasSimple(): Boolean = fieldSimple != null

    fun hasDynamic(): Boolean = fieldDynamic != null

    fun hasError(): Boolean = fieldError != null
}

class IrTypeTable(
    val typesList: List<IrType>
) {

    fun hasTypes(): Boolean = true

    val typesCount: Int
        get() = typesList.size
}

class IrBreak(
    val loopId: Int,
    private val fieldLabel: IrDataIndex?
) {

    val label: IrDataIndex
        get() = fieldLabel!!

    fun hasLoopId(): Boolean = true

    fun hasLabel(): Boolean = fieldLabel != null
}

class IrBlock(
    private val fieldOrigin: IrStatementOrigin?,
    val statementList: List<IrStatement>
) {

    val origin: IrStatementOrigin
        get() = fieldOrigin!!

    fun hasOrigin(): Boolean = fieldOrigin != null

    fun hasStatement(): Boolean = true

    val statementCount: Int
        get() = statementList.size
}

class MemberAccessCommon(
    private val fieldDispatchReceiver: IrExpression?,
    private val fieldExtensionReceiver: IrExpression?,
    val valueArgumentList: List<NullableIrExpression>,
    val typeArguments: TypeArguments
) {

    val dispatchReceiver: IrExpression
        get() = fieldDispatchReceiver!!

    val extensionReceiver: IrExpression
        get() = fieldExtensionReceiver!!

    fun hasDispatchReceiver(): Boolean = fieldDispatchReceiver != null

    fun hasExtensionReceiver(): Boolean = fieldExtensionReceiver != null

    fun hasValueArgument(): Boolean = true

    val valueArgumentCount: Int
        get() = valueArgumentList.size

    fun hasTypeArguments(): Boolean = true
}

class IrCall(
    val symbol: IrDataIndex,
    val memberAccess: MemberAccessCommon,
    private val fieldSuper: IrDataIndex?,
    private val fieldOrigin: IrStatementOrigin?
) {

    val `super`: IrDataIndex
        get() = fieldSuper!!

    val origin: IrStatementOrigin
        get() = fieldOrigin!!

    fun hasSymbol(): Boolean = true

    fun hasMemberAccess(): Boolean = true

    fun hasSuper(): Boolean = fieldSuper != null

    fun hasOrigin(): Boolean = fieldOrigin != null
}

class IrConstructorCall(
    val symbol: IrDataIndex,
    val constructorTypeArgumentsCount: Int,
    val memberAccess: MemberAccessCommon
) {

    fun hasSymbol(): Boolean = true

    fun hasConstructorTypeArgumentsCount(): Boolean = true

    fun hasMemberAccess(): Boolean = true
}

class IrFunctionReference(
    val symbol: IrDataIndex,
    private val fieldOrigin: IrStatementOrigin?,
    val memberAccess: MemberAccessCommon
) {

    val origin: IrStatementOrigin
        get() = fieldOrigin!!

    fun hasSymbol(): Boolean = true

    fun hasOrigin(): Boolean = fieldOrigin != null

    fun hasMemberAccess(): Boolean = true
}

class IrLocalDelegatedPropertyReference(
    val delegate: IrDataIndex,
    private val fieldGetter: IrDataIndex?,
    private val fieldSetter: IrDataIndex?,
    val symbol: IrDataIndex,
    private val fieldOrigin: IrStatementOrigin?
) {

    val getter: IrDataIndex
        get() = fieldGetter!!

    val setter: IrDataIndex
        get() = fieldSetter!!

    val origin: IrStatementOrigin
        get() = fieldOrigin!!

    fun hasDelegate(): Boolean = true

    fun hasGetter(): Boolean = fieldGetter != null

    fun hasSetter(): Boolean = fieldSetter != null

    fun hasSymbol(): Boolean = true

    fun hasOrigin(): Boolean = fieldOrigin != null
}

class IrPropertyReference(
    private val fieldField: IrDataIndex?,
    private val fieldGetter: IrDataIndex?,
    private val fieldSetter: IrDataIndex?,
    private val fieldOrigin: IrStatementOrigin?,
    val memberAccess: MemberAccessCommon,
    val symbol: IrDataIndex
) {

    val field: IrDataIndex
        get() = fieldField!!

    val getter: IrDataIndex
        get() = fieldGetter!!

    val setter: IrDataIndex
        get() = fieldSetter!!

    val origin: IrStatementOrigin
        get() = fieldOrigin!!

    fun hasField(): Boolean = fieldField != null

    fun hasGetter(): Boolean = fieldGetter != null

    fun hasSetter(): Boolean = fieldSetter != null

    fun hasOrigin(): Boolean = fieldOrigin != null

    fun hasMemberAccess(): Boolean = true

    fun hasSymbol(): Boolean = true
}

class IrComposite(
    val statementList: List<IrStatement>,
    private val fieldOrigin: IrStatementOrigin?
) {

    val origin: IrStatementOrigin
        get() = fieldOrigin!!

    fun hasStatement(): Boolean = true

    val statementCount: Int
        get() = statementList.size

    fun hasOrigin(): Boolean = fieldOrigin != null
}

class IrClassReference(
    val classSymbol: IrDataIndex,
    val classType: IrDataIndex
) {

    fun hasClassSymbol(): Boolean = true

    fun hasClassType(): Boolean = true
}

class IrConst(
    val valueCase: ValueCase,
    private val fieldNull: Boolean?,
    private val fieldBoolean: Boolean?,
    private val fieldChar: Int?,
    private val fieldByte: Int?,
    private val fieldShort: Int?,
    private val fieldInt: Int?,
    private val fieldLong: Long?,
    private val fieldFloat: Float?,
    private val fieldDouble: Double?,
    private val fieldString: IrDataIndex?
) {
    enum class ValueCase {
        NULL,
        BOOLEAN,
        CHAR,
        BYTE,
        SHORT,
        INT,
        LONG,
        FLOAT,
        DOUBLE,
        STRING,
        VALUE_NOT_SET
    }

    val `null`: Boolean
        get() = fieldNull!!

    val boolean: Boolean
        get() = fieldBoolean!!

    val char: Int
        get() = fieldChar!!

    val byte: Int
        get() = fieldByte!!

    val short: Int
        get() = fieldShort!!

    val int: Int
        get() = fieldInt!!

    val long: Long
        get() = fieldLong!!

    val float: Float
        get() = fieldFloat!!

    val double: Double
        get() = fieldDouble!!

    val string: IrDataIndex
        get() = fieldString!!

    fun hasNull(): Boolean = fieldNull != null

    fun hasBoolean(): Boolean = fieldBoolean != null

    fun hasChar(): Boolean = fieldChar != null

    fun hasByte(): Boolean = fieldByte != null

    fun hasShort(): Boolean = fieldShort != null

    fun hasInt(): Boolean = fieldInt != null

    fun hasLong(): Boolean = fieldLong != null

    fun hasFloat(): Boolean = fieldFloat != null

    fun hasDouble(): Boolean = fieldDouble != null

    fun hasString(): Boolean = fieldString != null
}

class IrContinue(
    val loopId: Int,
    private val fieldLabel: IrDataIndex?
) {

    val label: IrDataIndex
        get() = fieldLabel!!

    fun hasLoopId(): Boolean = true

    fun hasLabel(): Boolean = fieldLabel != null
}

class IrDelegatingConstructorCall(
    val symbol: IrDataIndex,
    val memberAccess: MemberAccessCommon
) {

    fun hasSymbol(): Boolean = true

    fun hasMemberAccess(): Boolean = true
}

class IrDoWhile(
    val loop: Loop
) {

    fun hasLoop(): Boolean = true
}

class IrEnumConstructorCall(
    val symbol: IrDataIndex,
    val memberAccess: MemberAccessCommon
) {

    fun hasSymbol(): Boolean = true

    fun hasMemberAccess(): Boolean = true
}

class IrGetClass(
    val argument: IrExpression
) {

    fun hasArgument(): Boolean = true
}

class IrGetEnumValue(
    val symbol: IrDataIndex
) {

    fun hasSymbol(): Boolean = true
}

class FieldAccessCommon(
    val symbol: IrDataIndex,
    private val fieldSuper: IrDataIndex?,
    private val fieldReceiver: IrExpression?
) {

    val `super`: IrDataIndex
        get() = fieldSuper!!

    val receiver: IrExpression
        get() = fieldReceiver!!

    fun hasSymbol(): Boolean = true

    fun hasSuper(): Boolean = fieldSuper != null

    fun hasReceiver(): Boolean = fieldReceiver != null
}

class IrGetField(
    val fieldAccess: FieldAccessCommon,
    private val fieldOrigin: IrStatementOrigin?
) {

    val origin: IrStatementOrigin
        get() = fieldOrigin!!

    fun hasFieldAccess(): Boolean = true

    fun hasOrigin(): Boolean = fieldOrigin != null
}

class IrGetValue(
    val symbol: IrDataIndex,
    private val fieldOrigin: IrStatementOrigin?
) {

    val origin: IrStatementOrigin
        get() = fieldOrigin!!

    fun hasSymbol(): Boolean = true

    fun hasOrigin(): Boolean = fieldOrigin != null
}

class IrGetObject(
    val symbol: IrDataIndex
) {

    fun hasSymbol(): Boolean = true
}

class IrInstanceInitializerCall(
    val symbol: IrDataIndex
) {

    fun hasSymbol(): Boolean = true
}

class Loop(
    val loopId: Int,
    val condition: IrExpression,
    private val fieldLabel: IrDataIndex?,
    private val fieldBody: IrExpression?,
    private val fieldOrigin: IrStatementOrigin?
) {

    val label: IrDataIndex
        get() = fieldLabel!!

    val body: IrExpression
        get() = fieldBody!!

    val origin: IrStatementOrigin
        get() = fieldOrigin!!

    fun hasLoopId(): Boolean = true

    fun hasCondition(): Boolean = true

    fun hasLabel(): Boolean = fieldLabel != null

    fun hasBody(): Boolean = fieldBody != null

    fun hasOrigin(): Boolean = fieldOrigin != null
}

class IrReturn(
    val returnTarget: IrDataIndex,
    val value: IrExpression
) {

    fun hasReturnTarget(): Boolean = true

    fun hasValue(): Boolean = true
}

class IrSetField(
    val fieldAccess: FieldAccessCommon,
    val value: IrExpression,
    private val fieldOrigin: IrStatementOrigin?
) {

    val origin: IrStatementOrigin
        get() = fieldOrigin!!

    fun hasFieldAccess(): Boolean = true

    fun hasValue(): Boolean = true

    fun hasOrigin(): Boolean = fieldOrigin != null
}

class IrSetVariable(
    val symbol: IrDataIndex,
    val value: IrExpression,
    private val fieldOrigin: IrStatementOrigin?
) {

    val origin: IrStatementOrigin
        get() = fieldOrigin!!

    fun hasSymbol(): Boolean = true

    fun hasValue(): Boolean = true

    fun hasOrigin(): Boolean = fieldOrigin != null
}

class IrSpreadElement(
    val expression: IrExpression,
    val coordinates: Coordinates
) {

    fun hasExpression(): Boolean = true

    fun hasCoordinates(): Boolean = true
}

class IrStringConcat(
    val argumentList: List<IrExpression>
) {

    fun hasArgument(): Boolean = true

    val argumentCount: Int
        get() = argumentList.size
}

class IrThrow(
    val value: IrExpression
) {

    fun hasValue(): Boolean = true
}

class IrTry(
    val result: IrExpression,
    val catchList: List<IrStatement>,
    private val fieldFinally: IrExpression?
) {

    val finally: IrExpression
        get() = fieldFinally!!

    fun hasResult(): Boolean = true

    fun hasCatch(): Boolean = true

    val catchCount: Int
        get() = catchList.size

    fun hasFinally(): Boolean = fieldFinally != null
}

class IrTypeOp(
    val operator: IrTypeOperator,
    val operand: IrDataIndex,
    val argument: IrExpression
) {

    fun hasOperator(): Boolean = true

    fun hasOperand(): Boolean = true

    fun hasArgument(): Boolean = true
}

class IrVararg(
    val elementType: IrDataIndex,
    val elementList: List<IrVarargElement>
) {

    fun hasElementType(): Boolean = true

    fun hasElement(): Boolean = true

    val elementCount: Int
        get() = elementList.size
}

class IrVarargElement(
    val varargElementCase: VarargElementCase,
    private val fieldExpression: IrExpression?,
    private val fieldSpreadElement: IrSpreadElement?
) {
    enum class VarargElementCase {
        EXPRESSION,
        SPREAD_ELEMENT,
        VARARG_ELEMENT_NOT_SET
    }

    val expression: IrExpression
        get() = fieldExpression!!

    val spreadElement: IrSpreadElement
        get() = fieldSpreadElement!!

    fun hasExpression(): Boolean = fieldExpression != null

    fun hasSpreadElement(): Boolean = fieldSpreadElement != null
}

class IrWhen(
    val branchList: List<IrStatement>,
    private val fieldOrigin: IrStatementOrigin?
) {

    val origin: IrStatementOrigin
        get() = fieldOrigin!!

    fun hasBranch(): Boolean = true

    val branchCount: Int
        get() = branchList.size

    fun hasOrigin(): Boolean = fieldOrigin != null
}

class IrWhile(
    val loop: Loop
) {

    fun hasLoop(): Boolean = true
}

class IrFunctionExpression(
    val function: IrFunction,
    val origin: IrStatementOrigin
) {

    fun hasFunction(): Boolean = true

    fun hasOrigin(): Boolean = true
}

class IrDynamicMemberExpression(
    val memberName: IrDataIndex,
    val receiver: IrExpression
) {

    fun hasMemberName(): Boolean = true

    fun hasReceiver(): Boolean = true
}

class IrDynamicOperatorExpression(
    val operator: IrDynamicOperator,
    val receiver: IrExpression,
    val argumentList: List<IrExpression>
) {
    enum class IrDynamicOperator {
        UNARY_PLUS,
        UNARY_MINUS,
        EXCL,
        PREFIX_INCREMENT,
        POSTFIX_INCREMENT,
        PREFIX_DECREMENT,
        POSTFIX_DECREMENT,
        BINARY_PLUS,
        BINARY_MINUS,
        MUL,
        DIV,
        MOD,
        GT,
        LT,
        GE,
        LE,
        EQEQ,
        EXCLEQ,
        EQEQEQ,
        EXCLEQEQ,
        ANDAND,
        OROR,
        EQ,
        PLUSEQ,
        MINUSEQ,
        MULEQ,
        DIVEQ,
        MODEQ,
        ARRAY_ACCESS,
        INVOKE    ;

        companion object {
            fun fromIndex(index: Int): IrDynamicOperator {
                return when (index) {
                    1 -> UNARY_PLUS
                    2 -> UNARY_MINUS
                    3 -> EXCL
                    4 -> PREFIX_INCREMENT
                    5 -> POSTFIX_INCREMENT
                    6 -> PREFIX_DECREMENT
                    7 -> POSTFIX_DECREMENT
                    8 -> BINARY_PLUS
                    9 -> BINARY_MINUS
                    10 -> MUL
                    11 -> DIV
                    12 -> MOD
                    13 -> GT
                    14 -> LT
                    15 -> GE
                    16 -> LE
                    17 -> EQEQ
                    18 -> EXCLEQ
                    19 -> EQEQEQ
                    20 -> EXCLEQEQ
                    21 -> ANDAND
                    22 -> OROR
                    23 -> EQ
                    24 -> PLUSEQ
                    25 -> MINUSEQ
                    26 -> MULEQ
                    27 -> DIVEQ
                    28 -> MODEQ
                    29 -> ARRAY_ACCESS
                    30 -> INVOKE
                    else -> error("Unexpected enum value '$index' for enum 'IrDynamicOperator'")
                }
            }
        }
    }


    fun hasOperator(): Boolean = true

    fun hasReceiver(): Boolean = true

    fun hasArgument(): Boolean = true

    val argumentCount: Int
        get() = argumentList.size
}

class IrOperation(
    val operationCase: OperationCase,
    private val fieldBlock: IrBlock?,
    private val fieldBreak: IrBreak?,
    private val fieldCall: IrCall?,
    private val fieldClassReference: IrClassReference?,
    private val fieldComposite: IrComposite?,
    private val fieldConst: IrConst?,
    private val fieldContinue: IrContinue?,
    private val fieldDelegatingConstructorCall: IrDelegatingConstructorCall?,
    private val fieldDoWhile: IrDoWhile?,
    private val fieldEnumConstructorCall: IrEnumConstructorCall?,
    private val fieldFunctionReference: IrFunctionReference?,
    private val fieldGetClass: IrGetClass?,
    private val fieldGetEnumValue: IrGetEnumValue?,
    private val fieldGetField: IrGetField?,
    private val fieldGetObject: IrGetObject?,
    private val fieldGetValue: IrGetValue?,
    private val fieldInstanceInitializerCall: IrInstanceInitializerCall?,
    private val fieldPropertyReference: IrPropertyReference?,
    private val fieldReturn: IrReturn?,
    private val fieldSetField: IrSetField?,
    private val fieldSetVariable: IrSetVariable?,
    private val fieldStringConcat: IrStringConcat?,
    private val fieldThrow: IrThrow?,
    private val fieldTry: IrTry?,
    private val fieldTypeOp: IrTypeOp?,
    private val fieldVararg: IrVararg?,
    private val fieldWhen: IrWhen?,
    private val fieldWhile: IrWhile?,
    private val fieldDynamicMember: IrDynamicMemberExpression?,
    private val fieldDynamicOperator: IrDynamicOperatorExpression?,
    private val fieldLocalDelegatedPropertyReference: IrLocalDelegatedPropertyReference?,
    private val fieldConstructorCall: IrConstructorCall?,
    private val fieldFunctionExpression: IrFunctionExpression?
) {
    enum class OperationCase {
        BLOCK,
        BREAK,
        CALL,
        CLASS_REFERENCE,
        COMPOSITE,
        CONST,
        CONTINUE,
        DELEGATING_CONSTRUCTOR_CALL,
        DO_WHILE,
        ENUM_CONSTRUCTOR_CALL,
        FUNCTION_REFERENCE,
        GET_CLASS,
        GET_ENUM_VALUE,
        GET_FIELD,
        GET_OBJECT,
        GET_VALUE,
        INSTANCE_INITIALIZER_CALL,
        PROPERTY_REFERENCE,
        RETURN,
        SET_FIELD,
        SET_VARIABLE,
        STRING_CONCAT,
        THROW,
        TRY,
        TYPE_OP,
        VARARG,
        WHEN,
        WHILE,
        DYNAMIC_MEMBER,
        DYNAMIC_OPERATOR,
        LOCAL_DELEGATED_PROPERTY_REFERENCE,
        CONSTRUCTOR_CALL,
        FUNCTION_EXPRESSION,
        OPERATION_NOT_SET
    }

    val block: IrBlock
        get() = fieldBlock!!

    val `break`: IrBreak
        get() = fieldBreak!!

    val call: IrCall
        get() = fieldCall!!

    val classReference: IrClassReference
        get() = fieldClassReference!!

    val composite: IrComposite
        get() = fieldComposite!!

    val const: IrConst
        get() = fieldConst!!

    val `continue`: IrContinue
        get() = fieldContinue!!

    val delegatingConstructorCall: IrDelegatingConstructorCall
        get() = fieldDelegatingConstructorCall!!

    val doWhile: IrDoWhile
        get() = fieldDoWhile!!

    val enumConstructorCall: IrEnumConstructorCall
        get() = fieldEnumConstructorCall!!

    val functionReference: IrFunctionReference
        get() = fieldFunctionReference!!

    val getClass: IrGetClass
        get() = fieldGetClass!!

    val getEnumValue: IrGetEnumValue
        get() = fieldGetEnumValue!!

    val getField: IrGetField
        get() = fieldGetField!!

    val getObject: IrGetObject
        get() = fieldGetObject!!

    val getValue: IrGetValue
        get() = fieldGetValue!!

    val instanceInitializerCall: IrInstanceInitializerCall
        get() = fieldInstanceInitializerCall!!

    val propertyReference: IrPropertyReference
        get() = fieldPropertyReference!!

    val `return`: IrReturn
        get() = fieldReturn!!

    val setField: IrSetField
        get() = fieldSetField!!

    val setVariable: IrSetVariable
        get() = fieldSetVariable!!

    val stringConcat: IrStringConcat
        get() = fieldStringConcat!!

    val `throw`: IrThrow
        get() = fieldThrow!!

    val `try`: IrTry
        get() = fieldTry!!

    val typeOp: IrTypeOp
        get() = fieldTypeOp!!

    val vararg: IrVararg
        get() = fieldVararg!!

    val `when`: IrWhen
        get() = fieldWhen!!

    val `while`: IrWhile
        get() = fieldWhile!!

    val dynamicMember: IrDynamicMemberExpression
        get() = fieldDynamicMember!!

    val dynamicOperator: IrDynamicOperatorExpression
        get() = fieldDynamicOperator!!

    val localDelegatedPropertyReference: IrLocalDelegatedPropertyReference
        get() = fieldLocalDelegatedPropertyReference!!

    val constructorCall: IrConstructorCall
        get() = fieldConstructorCall!!

    val functionExpression: IrFunctionExpression
        get() = fieldFunctionExpression!!

    fun hasBlock(): Boolean = fieldBlock != null

    fun hasBreak(): Boolean = fieldBreak != null

    fun hasCall(): Boolean = fieldCall != null

    fun hasClassReference(): Boolean = fieldClassReference != null

    fun hasComposite(): Boolean = fieldComposite != null

    fun hasConst(): Boolean = fieldConst != null

    fun hasContinue(): Boolean = fieldContinue != null

    fun hasDelegatingConstructorCall(): Boolean = fieldDelegatingConstructorCall != null

    fun hasDoWhile(): Boolean = fieldDoWhile != null

    fun hasEnumConstructorCall(): Boolean = fieldEnumConstructorCall != null

    fun hasFunctionReference(): Boolean = fieldFunctionReference != null

    fun hasGetClass(): Boolean = fieldGetClass != null

    fun hasGetEnumValue(): Boolean = fieldGetEnumValue != null

    fun hasGetField(): Boolean = fieldGetField != null

    fun hasGetObject(): Boolean = fieldGetObject != null

    fun hasGetValue(): Boolean = fieldGetValue != null

    fun hasInstanceInitializerCall(): Boolean = fieldInstanceInitializerCall != null

    fun hasPropertyReference(): Boolean = fieldPropertyReference != null

    fun hasReturn(): Boolean = fieldReturn != null

    fun hasSetField(): Boolean = fieldSetField != null

    fun hasSetVariable(): Boolean = fieldSetVariable != null

    fun hasStringConcat(): Boolean = fieldStringConcat != null

    fun hasThrow(): Boolean = fieldThrow != null

    fun hasTry(): Boolean = fieldTry != null

    fun hasTypeOp(): Boolean = fieldTypeOp != null

    fun hasVararg(): Boolean = fieldVararg != null

    fun hasWhen(): Boolean = fieldWhen != null

    fun hasWhile(): Boolean = fieldWhile != null

    fun hasDynamicMember(): Boolean = fieldDynamicMember != null

    fun hasDynamicOperator(): Boolean = fieldDynamicOperator != null

    fun hasLocalDelegatedPropertyReference(): Boolean = fieldLocalDelegatedPropertyReference != null

    fun hasConstructorCall(): Boolean = fieldConstructorCall != null

    fun hasFunctionExpression(): Boolean = fieldFunctionExpression != null
}

enum class IrTypeOperator {
    CAST,
    IMPLICIT_CAST,
    IMPLICIT_NOTNULL,
    IMPLICIT_COERCION_TO_UNIT,
    IMPLICIT_INTEGER_COERCION,
    SAFE_CAST,
    INSTANCEOF,
    NOT_INSTANCEOF,
    SAM_CONVERSION,
    IMPLICIT_DYNAMIC_CAST;

    companion object {
        fun fromIndex(index: Int): IrTypeOperator {
            return when (index) {
                1 -> CAST
                2 -> IMPLICIT_CAST
                3 -> IMPLICIT_NOTNULL
                4 -> IMPLICIT_COERCION_TO_UNIT
                5 -> IMPLICIT_INTEGER_COERCION
                6 -> SAFE_CAST
                7 -> INSTANCEOF
                8 -> NOT_INSTANCEOF
                9 -> SAM_CONVERSION
                10 -> IMPLICIT_DYNAMIC_CAST
                else -> error("Unexpected enum value '$index' for enum 'IrTypeOperator'")
            }
        }
    }
}

class IrExpression(
    val operation: IrOperation,
    val type: IrDataIndex,
    val coordinates: Coordinates
) {

    fun hasOperation(): Boolean = true

    fun hasType(): Boolean = true

    fun hasCoordinates(): Boolean = true
}

class NullableIrExpression(
    private val fieldExpression: IrExpression?
) {

    val expression: IrExpression
        get() = fieldExpression!!

    fun hasExpression(): Boolean = fieldExpression != null
}

class IrDeclarationBase(
    val symbol: IrDataIndex,
    val origin: IrDeclarationOrigin,
    val coordinates: Coordinates,
    val annotations: Annotations
) {

    fun hasSymbol(): Boolean = true

    fun hasOrigin(): Boolean = true

    fun hasCoordinates(): Boolean = true

    fun hasAnnotations(): Boolean = true
}

class IrFunctionBase(
    val base: IrDeclarationBase,
    val name: IrDataIndex,
    val visibility: Visibility,
    val isInline: Boolean,
    val isExternal: Boolean,
    val typeParameters: IrTypeParameterContainer,
    private val fieldDispatchReceiver: IrValueParameter?,
    private val fieldExtensionReceiver: IrValueParameter?,
    val valueParameterList: List<IrValueParameter>,
    private val fieldBody: IrDataIndex?,
    val returnType: IrDataIndex
) {

    val dispatchReceiver: IrValueParameter
        get() = fieldDispatchReceiver!!

    val extensionReceiver: IrValueParameter
        get() = fieldExtensionReceiver!!

    val body: IrDataIndex
        get() = fieldBody!!

    fun hasBase(): Boolean = true

    fun hasName(): Boolean = true

    fun hasVisibility(): Boolean = true

    fun hasIsInline(): Boolean = true

    fun hasIsExternal(): Boolean = true

    fun hasTypeParameters(): Boolean = true

    fun hasDispatchReceiver(): Boolean = fieldDispatchReceiver != null

    fun hasExtensionReceiver(): Boolean = fieldExtensionReceiver != null

    fun hasValueParameter(): Boolean = true

    val valueParameterCount: Int
        get() = valueParameterList.size

    fun hasBody(): Boolean = fieldBody != null

    fun hasReturnType(): Boolean = true
}

class IrFunction(
    val base: IrFunctionBase,
    val modality: ModalityKind,
    val isTailrec: Boolean,
    val isSuspend: Boolean,
    val overriddenList: List<IrDataIndex>
) {

    fun hasBase(): Boolean = true

    fun hasModality(): Boolean = true

    fun hasIsTailrec(): Boolean = true

    fun hasIsSuspend(): Boolean = true

    fun hasOverridden(): Boolean = true

    val overriddenCount: Int
        get() = overriddenList.size
}

class IrConstructor(
    val base: IrFunctionBase,
    val isPrimary: Boolean
) {

    fun hasBase(): Boolean = true

    fun hasIsPrimary(): Boolean = true
}

class IrField(
    val base: IrDeclarationBase,
    private val fieldInitializer: IrDataIndex?,
    val name: IrDataIndex,
    val visibility: Visibility,
    val isFinal: Boolean,
    val isExternal: Boolean,
    val isStatic: Boolean,
    val type: IrDataIndex
) {

    val initializer: IrDataIndex
        get() = fieldInitializer!!

    fun hasBase(): Boolean = true

    fun hasInitializer(): Boolean = fieldInitializer != null

    fun hasName(): Boolean = true

    fun hasVisibility(): Boolean = true

    fun hasIsFinal(): Boolean = true

    fun hasIsExternal(): Boolean = true

    fun hasIsStatic(): Boolean = true

    fun hasType(): Boolean = true
}

class IrLocalDelegatedProperty(
    val base: IrDeclarationBase,
    val name: IrDataIndex,
    val type: IrDataIndex,
    val isVar: Boolean,
    val delegate: IrVariable,
    private val fieldGetter: IrFunction?,
    private val fieldSetter: IrFunction?
) {

    val getter: IrFunction
        get() = fieldGetter!!

    val setter: IrFunction
        get() = fieldSetter!!

    fun hasBase(): Boolean = true

    fun hasName(): Boolean = true

    fun hasType(): Boolean = true

    fun hasIsVar(): Boolean = true

    fun hasDelegate(): Boolean = true

    fun hasGetter(): Boolean = fieldGetter != null

    fun hasSetter(): Boolean = fieldSetter != null
}

class IrProperty(
    val base: IrDeclarationBase,
    val name: IrDataIndex,
    val visibility: Visibility,
    val modality: ModalityKind,
    val isVar: Boolean,
    val isConst: Boolean,
    val isLateinit: Boolean,
    val isDelegated: Boolean,
    val isExternal: Boolean,
    private val fieldBackingField: IrField?,
    private val fieldGetter: IrFunction?,
    private val fieldSetter: IrFunction?
) {

    val backingField: IrField
        get() = fieldBackingField!!

    val getter: IrFunction
        get() = fieldGetter!!

    val setter: IrFunction
        get() = fieldSetter!!

    fun hasBase(): Boolean = true

    fun hasName(): Boolean = true

    fun hasVisibility(): Boolean = true

    fun hasModality(): Boolean = true

    fun hasIsVar(): Boolean = true

    fun hasIsConst(): Boolean = true

    fun hasIsLateinit(): Boolean = true

    fun hasIsDelegated(): Boolean = true

    fun hasIsExternal(): Boolean = true

    fun hasBackingField(): Boolean = fieldBackingField != null

    fun hasGetter(): Boolean = fieldGetter != null

    fun hasSetter(): Boolean = fieldSetter != null
}

class IrVariable(
    val base: IrDeclarationBase,
    val name: IrDataIndex,
    val type: IrDataIndex,
    val isVar: Boolean,
    val isConst: Boolean,
    val isLateinit: Boolean,
    private val fieldInitializer: IrExpression?
) {

    val initializer: IrExpression
        get() = fieldInitializer!!

    fun hasBase(): Boolean = true

    fun hasName(): Boolean = true

    fun hasType(): Boolean = true

    fun hasIsVar(): Boolean = true

    fun hasIsConst(): Boolean = true

    fun hasIsLateinit(): Boolean = true

    fun hasInitializer(): Boolean = fieldInitializer != null
}

enum class ClassKind {
    CLASS,
    INTERFACE,
    ENUM_CLASS,
    ENUM_ENTRY,
    ANNOTATION_CLASS,
    OBJECT;

    companion object {
        fun fromIndex(index: Int): ClassKind {
            return when (index) {
                1 -> CLASS
                2 -> INTERFACE
                3 -> ENUM_CLASS
                4 -> ENUM_ENTRY
                5 -> ANNOTATION_CLASS
                6 -> OBJECT
                else -> error("Unexpected enum value '$index' for enum 'ClassKind'")
            }
        }
    }
}

enum class ModalityKind {
    FINAL_MODALITY,
    SEALED_MODALITY,
    OPEN_MODALITY,
    ABSTRACT_MODALITY;

    companion object {
        fun fromIndex(index: Int): ModalityKind {
            return when (index) {
                1 -> FINAL_MODALITY
                2 -> SEALED_MODALITY
                3 -> OPEN_MODALITY
                4 -> ABSTRACT_MODALITY
                else -> error("Unexpected enum value '$index' for enum 'ModalityKind'")
            }
        }
    }
}

class IrValueParameter(
    val base: IrDeclarationBase,
    val name: IrDataIndex,
    val index: Int,
    val type: IrDataIndex,
    private val fieldVarargElementType: IrDataIndex?,
    val isCrossinline: Boolean,
    val isNoinline: Boolean,
    private val fieldDefaultValue: IrDataIndex?
) {

    val varargElementType: IrDataIndex
        get() = fieldVarargElementType!!

    val defaultValue: IrDataIndex
        get() = fieldDefaultValue!!

    fun hasBase(): Boolean = true

    fun hasName(): Boolean = true

    fun hasIndex(): Boolean = true

    fun hasType(): Boolean = true

    fun hasVarargElementType(): Boolean = fieldVarargElementType != null

    fun hasIsCrossinline(): Boolean = true

    fun hasIsNoinline(): Boolean = true

    fun hasDefaultValue(): Boolean = fieldDefaultValue != null
}

class IrTypeParameter(
    val base: IrDeclarationBase,
    val name: IrDataIndex,
    val index: Int,
    val variance: IrTypeVariance,
    val superTypeList: List<IrDataIndex>,
    val isReified: Boolean
) {

    fun hasBase(): Boolean = true

    fun hasName(): Boolean = true

    fun hasIndex(): Boolean = true

    fun hasVariance(): Boolean = true

    fun hasSuperType(): Boolean = true

    val superTypeCount: Int
        get() = superTypeList.size

    fun hasIsReified(): Boolean = true
}

class IrTypeParameterContainer(
    val typeParameterList: List<IrTypeParameter>
) {

    fun hasTypeParameter(): Boolean = true

    val typeParameterCount: Int
        get() = typeParameterList.size
}

class IrClass(
    val base: IrDeclarationBase,
    val name: IrDataIndex,
    val kind: ClassKind,
    val visibility: Visibility,
    val modality: ModalityKind,
    val isCompanion: Boolean,
    val isInner: Boolean,
    val isData: Boolean,
    val isExternal: Boolean,
    val isInline: Boolean,
    private val fieldThisReceiver: IrValueParameter?,
    val typeParameters: IrTypeParameterContainer,
    val declarationContainer: IrDeclarationContainer,
    val superTypeList: List<IrDataIndex>
) {

    val thisReceiver: IrValueParameter
        get() = fieldThisReceiver!!

    fun hasBase(): Boolean = true

    fun hasName(): Boolean = true

    fun hasKind(): Boolean = true

    fun hasVisibility(): Boolean = true

    fun hasModality(): Boolean = true

    fun hasIsCompanion(): Boolean = true

    fun hasIsInner(): Boolean = true

    fun hasIsData(): Boolean = true

    fun hasIsExternal(): Boolean = true

    fun hasIsInline(): Boolean = true

    fun hasThisReceiver(): Boolean = fieldThisReceiver != null

    fun hasTypeParameters(): Boolean = true

    fun hasDeclarationContainer(): Boolean = true

    fun hasSuperType(): Boolean = true

    val superTypeCount: Int
        get() = superTypeList.size
}

class IrTypeAlias(
    val base: IrDeclarationBase,
    val name: IrDataIndex,
    val visibility: Visibility,
    val typeParameters: IrTypeParameterContainer,
    val expandedType: IrDataIndex,
    val isActual: Boolean
) {

    fun hasBase(): Boolean = true

    fun hasName(): Boolean = true

    fun hasVisibility(): Boolean = true

    fun hasTypeParameters(): Boolean = true

    fun hasExpandedType(): Boolean = true

    fun hasIsActual(): Boolean = true
}

class IrEnumEntry(
    val base: IrDeclarationBase,
    private val fieldInitializer: IrDataIndex?,
    private val fieldCorrespondingClass: IrClass?,
    val name: IrDataIndex
) {

    val initializer: IrDataIndex
        get() = fieldInitializer!!

    val correspondingClass: IrClass
        get() = fieldCorrespondingClass!!

    fun hasBase(): Boolean = true

    fun hasInitializer(): Boolean = fieldInitializer != null

    fun hasCorrespondingClass(): Boolean = fieldCorrespondingClass != null

    fun hasName(): Boolean = true
}

class IrAnonymousInit(
    val base: IrDeclarationBase,
    val body: IrDataIndex
) {

    fun hasBase(): Boolean = true

    fun hasBody(): Boolean = true
}

class IrDeclaration(
    val declaratorCase: DeclaratorCase,
    private val fieldIrAnonymousInit: IrAnonymousInit?,
    private val fieldIrClass: IrClass?,
    private val fieldIrConstructor: IrConstructor?,
    private val fieldIrEnumEntry: IrEnumEntry?,
    private val fieldIrField: IrField?,
    private val fieldIrFunction: IrFunction?,
    private val fieldIrProperty: IrProperty?,
    private val fieldIrTypeParameter: IrTypeParameter?,
    private val fieldIrVariable: IrVariable?,
    private val fieldIrValueParameter: IrValueParameter?,
    private val fieldIrLocalDelegatedProperty: IrLocalDelegatedProperty?,
    private val fieldIrTypeAlias: IrTypeAlias?
) {
    enum class DeclaratorCase {
        IR_ANONYMOUS_INIT,
        IR_CLASS,
        IR_CONSTRUCTOR,
        IR_ENUM_ENTRY,
        IR_FIELD,
        IR_FUNCTION,
        IR_PROPERTY,
        IR_TYPE_PARAMETER,
        IR_VARIABLE,
        IR_VALUE_PARAMETER,
        IR_LOCAL_DELEGATED_PROPERTY,
        IR_TYPE_ALIAS,
        DECLARATOR_NOT_SET
    }

    val irAnonymousInit: IrAnonymousInit
        get() = fieldIrAnonymousInit!!

    val irClass: IrClass
        get() = fieldIrClass!!

    val irConstructor: IrConstructor
        get() = fieldIrConstructor!!

    val irEnumEntry: IrEnumEntry
        get() = fieldIrEnumEntry!!

    val irField: IrField
        get() = fieldIrField!!

    val irFunction: IrFunction
        get() = fieldIrFunction!!

    val irProperty: IrProperty
        get() = fieldIrProperty!!

    val irTypeParameter: IrTypeParameter
        get() = fieldIrTypeParameter!!

    val irVariable: IrVariable
        get() = fieldIrVariable!!

    val irValueParameter: IrValueParameter
        get() = fieldIrValueParameter!!

    val irLocalDelegatedProperty: IrLocalDelegatedProperty
        get() = fieldIrLocalDelegatedProperty!!

    val irTypeAlias: IrTypeAlias
        get() = fieldIrTypeAlias!!

    fun hasIrAnonymousInit(): Boolean = fieldIrAnonymousInit != null

    fun hasIrClass(): Boolean = fieldIrClass != null

    fun hasIrConstructor(): Boolean = fieldIrConstructor != null

    fun hasIrEnumEntry(): Boolean = fieldIrEnumEntry != null

    fun hasIrField(): Boolean = fieldIrField != null

    fun hasIrFunction(): Boolean = fieldIrFunction != null

    fun hasIrProperty(): Boolean = fieldIrProperty != null

    fun hasIrTypeParameter(): Boolean = fieldIrTypeParameter != null

    fun hasIrVariable(): Boolean = fieldIrVariable != null

    fun hasIrValueParameter(): Boolean = fieldIrValueParameter != null

    fun hasIrLocalDelegatedProperty(): Boolean = fieldIrLocalDelegatedProperty != null

    fun hasIrTypeAlias(): Boolean = fieldIrTypeAlias != null
}

class IrBranch(
    val condition: IrExpression,
    val result: IrExpression
) {

    fun hasCondition(): Boolean = true

    fun hasResult(): Boolean = true
}

class IrBlockBody(
    val statementList: List<IrStatement>
) {

    fun hasStatement(): Boolean = true

    val statementCount: Int
        get() = statementList.size
}

class IrCatch(
    val catchParameter: IrVariable,
    val result: IrExpression
) {

    fun hasCatchParameter(): Boolean = true

    fun hasResult(): Boolean = true
}

enum class IrSyntheticBodyKind {
    ENUM_VALUES,
    ENUM_VALUEOF;

    companion object {
        fun fromIndex(index: Int): IrSyntheticBodyKind {
            return when (index) {
                1 -> ENUM_VALUES
                2 -> ENUM_VALUEOF
                else -> error("Unexpected enum value '$index' for enum 'IrSyntheticBodyKind'")
            }
        }
    }
}

class IrSyntheticBody(
    val kind: IrSyntheticBodyKind
) {

    fun hasKind(): Boolean = true
}

class IrStatement(
    val coordinates: Coordinates,
    val statementCase: StatementCase,
    private val fieldDeclaration: IrDeclaration?,
    private val fieldExpression: IrExpression?,
    private val fieldBlockBody: IrBlockBody?,
    private val fieldBranch: IrBranch?,
    private val fieldCatch: IrCatch?,
    private val fieldSyntheticBody: IrSyntheticBody?
) {
    enum class StatementCase {
        DECLARATION,
        EXPRESSION,
        BLOCK_BODY,
        BRANCH,
        CATCH,
        SYNTHETIC_BODY,
        STATEMENT_NOT_SET
    }

    val declaration: IrDeclaration
        get() = fieldDeclaration!!

    val expression: IrExpression
        get() = fieldExpression!!

    val blockBody: IrBlockBody
        get() = fieldBlockBody!!

    val branch: IrBranch
        get() = fieldBranch!!

    val catch: IrCatch
        get() = fieldCatch!!

    val syntheticBody: IrSyntheticBody
        get() = fieldSyntheticBody!!

    fun hasCoordinates(): Boolean = true

    fun hasDeclaration(): Boolean = fieldDeclaration != null

    fun hasExpression(): Boolean = fieldExpression != null

    fun hasBlockBody(): Boolean = fieldBlockBody != null

    fun hasBranch(): Boolean = fieldBranch != null

    fun hasCatch(): Boolean = fieldCatch != null

    fun hasSyntheticBody(): Boolean = fieldSyntheticBody != null
}

class IrProtoReaderMimic(private val source: ByteArray) {


    private var offset = 0

    private var currentEnd = source.size

    private val hasData: Boolean
        get() = offset < currentEnd

    private inline fun <T> readWithLength(block: () -> T): T {
        val length = readInt32()
        val oldEnd = currentEnd
        currentEnd = offset + length
        try {
            return block()
        } finally {
            currentEnd = oldEnd
        }
    }

    private fun nextByte(): Byte {
        if (!hasData) error("Oops")
        return source[offset++]
    }

    private fun readVarint64(): Long {
        var result = 0L

        var shift = 0
        while (true) {
            val b = nextByte().toInt()

            result = result or ((b and 0x7F).toLong() shl shift)
            shift += 7

            if ((b and 0x80) == 0) break
        }

        if (shift > 70) {
            error("int64 overflow $shift")
        }

        return result
    }

    private fun readVarint32(): Int {
        var result = 0

        var shift = 0
        while (true) {
            val b = nextByte().toInt()

            result = result or ((b and 0x7F) shl shift)
            shift += 7

            if ((b and 0x80) == 0) break
        }

        if (shift > 70) {
            error("int32 overflow $shift")
        }

        return result
    }

    private fun readInt32(): Int = readVarint32()

    private fun readInt64(): Long = readVarint64()

    private fun readBool(): Boolean = readVarint32() != 0

    private fun readFloat(): Float {
        var bits = nextByte().toInt()
        bits = (bits shl 8) or nextByte().toInt()
        bits = (bits shl 8) or nextByte().toInt()
        bits = (bits shl 8) or nextByte().toInt()

        return Float.fromBits(bits)
    }

    private fun readDouble(): Double {
        var bits = nextByte().toLong()
        bits = (bits shl 8) or nextByte().toLong()
        bits = (bits shl 8) or nextByte().toLong()
        bits = (bits shl 8) or nextByte().toLong()
        bits = (bits shl 8) or nextByte().toLong()
        bits = (bits shl 8) or nextByte().toLong()
        bits = (bits shl 8) or nextByte().toLong()
        bits = (bits shl 8) or nextByte().toLong()

        return Double.fromBits(bits)
    }

    private fun readString(): String {
        val length = readInt32()
        val result = String(source, offset, length)
        offset += length
        return result
    }

    private fun skip(type: Int) {
        when (type) {
            0 -> readInt64()
            1 -> offset += 8
            2 -> {
                val len = readInt32()
                offset += len
            }
            3, 4 -> error("groups")
            5 -> offset += 4
        }
    }


    fun readDescriptorReference(): DescriptorReference {
        var packageFqName: FqName? = null
        var classFqName: FqName? = null
        var name: IrDataIndex? = null
        var uniqId: UniqId? = null
        var isGetter: Boolean = false
        var isSetter: Boolean = false
        var isBackingField: Boolean = false
        var isFakeOverride: Boolean = false
        var isDefaultConstructor: Boolean = false
        var isEnumEntry: Boolean = false
        var isEnumSpecial: Boolean = false
        var isTypeParameter: Boolean = false
        while (hasData) {
            when (val fieldHeader = readInt32()) {
                10 -> packageFqName = readWithLength { readFqName() }
                18 -> classFqName = readWithLength { readFqName() }
                26 -> name = readWithLength { readIrDataIndex() }
                34 -> uniqId = readWithLength { readUniqId() }
                40 -> isGetter = readBool()
                48 -> isSetter = readBool()
                56 -> isBackingField = readBool()
                64 -> isFakeOverride = readBool()
                72 -> isDefaultConstructor = readBool()
                80 -> isEnumEntry = readBool()
                88 -> isEnumSpecial = readBool()
                96 -> isTypeParameter = readBool()
                else -> skip(fieldHeader and 7)
            }
        }
        return DescriptorReference(packageFqName!!, classFqName!!, name!!, uniqId, isGetter, isSetter, isBackingField, isFakeOverride, isDefaultConstructor, isEnumEntry, isEnumSpecial, isTypeParameter)
    }

    fun readUniqId(): UniqId {
        var index: Long = 0L
        var isLocal: Boolean = false
        while (hasData) {
            when (val fieldHeader = readInt32()) {
                8 -> index = readInt64()
                16 -> isLocal = readBool()
                else -> skip(fieldHeader and 7)
            }
        }
        return UniqId(index, isLocal)
    }

    fun readCoordinates(): Coordinates {
        var startOffset: Int = 0
        var endOffset: Int = 0
        while (hasData) {
            when (val fieldHeader = readInt32()) {
                8 -> startOffset = readInt32()
                16 -> endOffset = readInt32()
                else -> skip(fieldHeader and 7)
            }
        }
        return Coordinates(startOffset, endOffset)
    }

    fun readVisibility(): Visibility {
        var name: IrDataIndex? = null
        while (hasData) {
            when (val fieldHeader = readInt32()) {
                10 -> name = readWithLength { readIrDataIndex() }
                else -> skip(fieldHeader and 7)
            }
        }
        return Visibility(name!!)
    }

    fun readIrStatementOrigin(): IrStatementOrigin {
        var name: IrDataIndex? = null
        while (hasData) {
            when (val fieldHeader = readInt32()) {
                10 -> name = readWithLength { readIrDataIndex() }
                else -> skip(fieldHeader and 7)
            }
        }
        return IrStatementOrigin(name!!)
    }

    fun readIrDeclarationOrigin(): IrDeclarationOrigin {
        var origin: KnownOrigin? = null
        var custom: IrDataIndex? = null
        var oneOfCase: IrDeclarationOrigin.EitherCase = IrDeclarationOrigin.EitherCase.EITHER_NOT_SET
        while (hasData) {
            when (val fieldHeader = readInt32()) {
                8 -> {
                    origin = KnownOrigin.fromIndex(readInt32())
                    oneOfCase = IrDeclarationOrigin.EitherCase.ORIGIN
                }
                18 -> {
                    custom = readWithLength { readIrDataIndex() }
                    oneOfCase = IrDeclarationOrigin.EitherCase.CUSTOM
                }
                else -> skip(fieldHeader and 7)
            }
        }
        return IrDeclarationOrigin(oneOfCase!!, origin, custom)
    }

    fun readIrDataIndex(): IrDataIndex {
        var index: Int = 0
        while (hasData) {
            when (val fieldHeader = readInt32()) {
                8 -> index = readInt32()
                else -> skip(fieldHeader and 7)
            }
        }
        return IrDataIndex(index)
    }

    fun readFqName(): FqName {
        var segment: MutableList<IrDataIndex> = mutableListOf()
        while (hasData) {
            when (val fieldHeader = readInt32()) {
                10 -> segment.add(readWithLength { readIrDataIndex() })
                else -> skip(fieldHeader and 7)
            }
        }
        return FqName(segment)
    }

    fun readIrDeclarationContainer(): IrDeclarationContainer {
        var declaration: MutableList<IrDeclaration> = mutableListOf()
        while (hasData) {
            when (val fieldHeader = readInt32()) {
                10 -> declaration.add(readWithLength { readIrDeclaration() })
                else -> skip(fieldHeader and 7)
            }
        }
        return IrDeclarationContainer(declaration)
    }

    fun readFileEntry(): FileEntry {
        var name: String = ""
        var lineStartOffsets: MutableList<Int> = mutableListOf()
        while (hasData) {
            when (val fieldHeader = readInt32()) {
                10 -> name = readString()
                16 -> lineStartOffsets.add(readInt32())
                else -> skip(fieldHeader and 7)
            }
        }
        return FileEntry(name, lineStartOffsets)
    }

    fun readIrFile(): IrFile {
        var declarationId: MutableList<UniqId> = mutableListOf()
        var fileEntry: FileEntry? = null
        var fqName: FqName? = null
        var annotations: Annotations? = null
        var explicitlyExportedToCompiler: MutableList<IrDataIndex> = mutableListOf()
        while (hasData) {
            when (val fieldHeader = readInt32()) {
                10 -> declarationId.add(readWithLength { readUniqId() })
                18 -> fileEntry = readWithLength { readFileEntry() }
                26 -> fqName = readWithLength { readFqName() }
                34 -> annotations = readWithLength { readAnnotations() }
                42 -> explicitlyExportedToCompiler.add(readWithLength { readIrDataIndex() })
                else -> skip(fieldHeader and 7)
            }
        }
        return IrFile(declarationId, fileEntry!!, fqName!!, annotations!!, explicitlyExportedToCompiler)
    }

    fun readStringTable(): StringTable {
        var strings: MutableList<String> = mutableListOf()
        while (hasData) {
            when (val fieldHeader = readInt32()) {
                10 -> strings.add(readString())
                else -> skip(fieldHeader and 7)
            }
        }
        return StringTable(strings)
    }

    fun readIrSymbolData(): IrSymbolData {
        var kind: IrSymbolKind? = null
        var uniqId: UniqId? = null
        var topLevelUniqId: UniqId? = null
        var fqname: FqName? = null
        var descriptorReference: DescriptorReference? = null
        while (hasData) {
            when (val fieldHeader = readInt32()) {
                8 -> kind = IrSymbolKind.fromIndex(readInt32())
                18 -> uniqId = readWithLength { readUniqId() }
                26 -> topLevelUniqId = readWithLength { readUniqId() }
                34 -> fqname = readWithLength { readFqName() }
                42 -> descriptorReference = readWithLength { readDescriptorReference() }
                else -> skip(fieldHeader and 7)
            }
        }
        return IrSymbolData(kind!!, uniqId!!, topLevelUniqId!!, fqname, descriptorReference)
    }

    fun readIrSymbolTable(): IrSymbolTable {
        var symbols: MutableList<IrSymbolData> = mutableListOf()
        while (hasData) {
            when (val fieldHeader = readInt32()) {
                10 -> symbols.add(readWithLength { readIrSymbolData() })
                else -> skip(fieldHeader and 7)
            }
        }
        return IrSymbolTable(symbols)
    }

    fun readAnnotations(): Annotations {
        var annotation: MutableList<IrConstructorCall> = mutableListOf()
        while (hasData) {
            when (val fieldHeader = readInt32()) {
                10 -> annotation.add(readWithLength { readIrConstructorCall() })
                else -> skip(fieldHeader and 7)
            }
        }
        return Annotations(annotation)
    }

    fun readTypeArguments(): TypeArguments {
        var typeArgument: MutableList<IrDataIndex> = mutableListOf()
        while (hasData) {
            when (val fieldHeader = readInt32()) {
                10 -> typeArgument.add(readWithLength { readIrDataIndex() })
                else -> skip(fieldHeader and 7)
            }
        }
        return TypeArguments(typeArgument)
    }

    fun readIrStarProjection(): IrStarProjection {
        var void: Boolean? = null
        while (hasData) {
            when (val fieldHeader = readInt32()) {
                8 -> void = readBool()
                else -> skip(fieldHeader and 7)
            }
        }
        return IrStarProjection(void)
    }

    fun readIrTypeProjection(): IrTypeProjection {
        var variance: IrTypeVariance? = null
        var type: IrDataIndex? = null
        while (hasData) {
            when (val fieldHeader = readInt32()) {
                8 -> variance = IrTypeVariance.fromIndex(readInt32())
                18 -> type = readWithLength { readIrDataIndex() }
                else -> skip(fieldHeader and 7)
            }
        }
        return IrTypeProjection(variance!!, type!!)
    }

    fun readIrTypeArgument(): IrTypeArgument {
        var star: IrStarProjection? = null
        var type: IrTypeProjection? = null
        var oneOfCase: IrTypeArgument.KindCase = IrTypeArgument.KindCase.KIND_NOT_SET
        while (hasData) {
            when (val fieldHeader = readInt32()) {
                10 -> {
                    star = readWithLength { readIrStarProjection() }
                    oneOfCase = IrTypeArgument.KindCase.STAR
                }
                18 -> {
                    type = readWithLength { readIrTypeProjection() }
                    oneOfCase = IrTypeArgument.KindCase.TYPE
                }
                else -> skip(fieldHeader and 7)
            }
        }
        return IrTypeArgument(oneOfCase!!, star, type)
    }

    fun readIrSimpleType(): IrSimpleType {
        var annotations: Annotations? = null
        var classifier: IrDataIndex? = null
        var hasQuestionMark: Boolean = false
        var argument: MutableList<IrTypeArgument> = mutableListOf()
        var abbreviation: IrTypeAbbreviation? = null
        while (hasData) {
            when (val fieldHeader = readInt32()) {
                10 -> annotations = readWithLength { readAnnotations() }
                18 -> classifier = readWithLength { readIrDataIndex() }
                24 -> hasQuestionMark = readBool()
                34 -> argument.add(readWithLength { readIrTypeArgument() })
                42 -> abbreviation = readWithLength { readIrTypeAbbreviation() }
                else -> skip(fieldHeader and 7)
            }
        }
        return IrSimpleType(annotations!!, classifier!!, hasQuestionMark, argument, abbreviation)
    }

    fun readIrTypeAbbreviation(): IrTypeAbbreviation {
        var annotations: Annotations? = null
        var typeAlias: IrDataIndex? = null
        var hasQuestionMark: Boolean = false
        var argument: MutableList<IrTypeArgument> = mutableListOf()
        while (hasData) {
            when (val fieldHeader = readInt32()) {
                10 -> annotations = readWithLength { readAnnotations() }
                18 -> typeAlias = readWithLength { readIrDataIndex() }
                24 -> hasQuestionMark = readBool()
                34 -> argument.add(readWithLength { readIrTypeArgument() })
                else -> skip(fieldHeader and 7)
            }
        }
        return IrTypeAbbreviation(annotations!!, typeAlias!!, hasQuestionMark, argument)
    }

    fun readIrDynamicType(): IrDynamicType {
        var annotations: Annotations? = null
        while (hasData) {
            when (val fieldHeader = readInt32()) {
                10 -> annotations = readWithLength { readAnnotations() }
                else -> skip(fieldHeader and 7)
            }
        }
        return IrDynamicType(annotations!!)
    }

    fun readIrErrorType(): IrErrorType {
        var annotations: Annotations? = null
        while (hasData) {
            when (val fieldHeader = readInt32()) {
                10 -> annotations = readWithLength { readAnnotations() }
                else -> skip(fieldHeader and 7)
            }
        }
        return IrErrorType(annotations!!)
    }

    fun readIrType(): IrType {
        var simple: IrSimpleType? = null
        var dynamic: IrDynamicType? = null
        var error: IrErrorType? = null
        var oneOfCase: IrType.KindCase = IrType.KindCase.KIND_NOT_SET
        while (hasData) {
            when (val fieldHeader = readInt32()) {
                10 -> {
                    simple = readWithLength { readIrSimpleType() }
                    oneOfCase = IrType.KindCase.SIMPLE
                }
                18 -> {
                    dynamic = readWithLength { readIrDynamicType() }
                    oneOfCase = IrType.KindCase.DYNAMIC
                }
                26 -> {
                    error = readWithLength { readIrErrorType() }
                    oneOfCase = IrType.KindCase.ERROR
                }
                else -> skip(fieldHeader and 7)
            }
        }
        return IrType(oneOfCase!!, simple, dynamic, error)
    }

    fun readIrTypeTable(): IrTypeTable {
        var types: MutableList<IrType> = mutableListOf()
        while (hasData) {
            when (val fieldHeader = readInt32()) {
                10 -> types.add(readWithLength { readIrType() })
                else -> skip(fieldHeader and 7)
            }
        }
        return IrTypeTable(types)
    }

    fun readIrBreak(): IrBreak {
        var loopId: Int = 0
        var label: IrDataIndex? = null
        while (hasData) {
            when (val fieldHeader = readInt32()) {
                8 -> loopId = readInt32()
                18 -> label = readWithLength { readIrDataIndex() }
                else -> skip(fieldHeader and 7)
            }
        }
        return IrBreak(loopId, label)
    }

    fun readIrBlock(): IrBlock {
        var origin: IrStatementOrigin? = null
        var statement: MutableList<IrStatement> = mutableListOf()
        while (hasData) {
            when (val fieldHeader = readInt32()) {
                10 -> origin = readWithLength { readIrStatementOrigin() }
                18 -> statement.add(readWithLength { readIrStatement() })
                else -> skip(fieldHeader and 7)
            }
        }
        return IrBlock(origin, statement)
    }

    fun readMemberAccessCommon(): MemberAccessCommon {
        var dispatchReceiver: IrExpression? = null
        var extensionReceiver: IrExpression? = null
        var valueArgument: MutableList<NullableIrExpression> = mutableListOf()
        var typeArguments: TypeArguments? = null
        while (hasData) {
            when (val fieldHeader = readInt32()) {
                10 -> dispatchReceiver = readWithLength { readIrExpression() }
                18 -> extensionReceiver = readWithLength { readIrExpression() }
                26 -> valueArgument.add(readWithLength { readNullableIrExpression() })
                34 -> typeArguments = readWithLength { readTypeArguments() }
                else -> skip(fieldHeader and 7)
            }
        }
        return MemberAccessCommon(dispatchReceiver, extensionReceiver, valueArgument, typeArguments!!)
    }

    fun readIrCall(): IrCall {
        var symbol: IrDataIndex? = null
        var memberAccess: MemberAccessCommon? = null
        var super_: IrDataIndex? = null
        var origin: IrStatementOrigin? = null
        while (hasData) {
            when (val fieldHeader = readInt32()) {
                10 -> symbol = readWithLength { readIrDataIndex() }
                18 -> memberAccess = readWithLength { readMemberAccessCommon() }
                26 -> super_ = readWithLength { readIrDataIndex() }
                34 -> origin = readWithLength { readIrStatementOrigin() }
                else -> skip(fieldHeader and 7)
            }
        }
        return IrCall(symbol!!, memberAccess!!, super_, origin)
    }

    fun readIrConstructorCall(): IrConstructorCall {
        var symbol: IrDataIndex? = null
        var constructorTypeArgumentsCount: Int = 0
        var memberAccess: MemberAccessCommon? = null
        while (hasData) {
            when (val fieldHeader = readInt32()) {
                10 -> symbol = readWithLength { readIrDataIndex() }
                16 -> constructorTypeArgumentsCount = readInt32()
                26 -> memberAccess = readWithLength { readMemberAccessCommon() }
                else -> skip(fieldHeader and 7)
            }
        }
        return IrConstructorCall(symbol!!, constructorTypeArgumentsCount, memberAccess!!)
    }

    fun readIrFunctionReference(): IrFunctionReference {
        var symbol: IrDataIndex? = null
        var origin: IrStatementOrigin? = null
        var memberAccess: MemberAccessCommon? = null
        while (hasData) {
            when (val fieldHeader = readInt32()) {
                10 -> symbol = readWithLength { readIrDataIndex() }
                18 -> origin = readWithLength { readIrStatementOrigin() }
                26 -> memberAccess = readWithLength { readMemberAccessCommon() }
                else -> skip(fieldHeader and 7)
            }
        }
        return IrFunctionReference(symbol!!, origin, memberAccess!!)
    }

    fun readIrLocalDelegatedPropertyReference(): IrLocalDelegatedPropertyReference {
        var delegate: IrDataIndex? = null
        var getter: IrDataIndex? = null
        var setter: IrDataIndex? = null
        var symbol: IrDataIndex? = null
        var origin: IrStatementOrigin? = null
        while (hasData) {
            when (val fieldHeader = readInt32()) {
                10 -> delegate = readWithLength { readIrDataIndex() }
                18 -> getter = readWithLength { readIrDataIndex() }
                26 -> setter = readWithLength { readIrDataIndex() }
                34 -> symbol = readWithLength { readIrDataIndex() }
                42 -> origin = readWithLength { readIrStatementOrigin() }
                else -> skip(fieldHeader and 7)
            }
        }
        return IrLocalDelegatedPropertyReference(delegate!!, getter, setter, symbol!!, origin)
    }

    fun readIrPropertyReference(): IrPropertyReference {
        var field: IrDataIndex? = null
        var getter: IrDataIndex? = null
        var setter: IrDataIndex? = null
        var origin: IrStatementOrigin? = null
        var memberAccess: MemberAccessCommon? = null
        var symbol: IrDataIndex? = null
        while (hasData) {
            when (val fieldHeader = readInt32()) {
                10 -> field = readWithLength { readIrDataIndex() }
                18 -> getter = readWithLength { readIrDataIndex() }
                26 -> setter = readWithLength { readIrDataIndex() }
                34 -> origin = readWithLength { readIrStatementOrigin() }
                42 -> memberAccess = readWithLength { readMemberAccessCommon() }
                50 -> symbol = readWithLength { readIrDataIndex() }
                else -> skip(fieldHeader and 7)
            }
        }
        return IrPropertyReference(field, getter, setter, origin, memberAccess!!, symbol!!)
    }

    fun readIrComposite(): IrComposite {
        var statement: MutableList<IrStatement> = mutableListOf()
        var origin: IrStatementOrigin? = null
        while (hasData) {
            when (val fieldHeader = readInt32()) {
                10 -> statement.add(readWithLength { readIrStatement() })
                18 -> origin = readWithLength { readIrStatementOrigin() }
                else -> skip(fieldHeader and 7)
            }
        }
        return IrComposite(statement, origin)
    }

    fun readIrClassReference(): IrClassReference {
        var classSymbol: IrDataIndex? = null
        var classType: IrDataIndex? = null
        while (hasData) {
            when (val fieldHeader = readInt32()) {
                10 -> classSymbol = readWithLength { readIrDataIndex() }
                18 -> classType = readWithLength { readIrDataIndex() }
                else -> skip(fieldHeader and 7)
            }
        }
        return IrClassReference(classSymbol!!, classType!!)
    }

    fun readIrConst(): IrConst {
        var null_: Boolean? = null
        var boolean: Boolean? = null
        var char: Int? = null
        var byte: Int? = null
        var short: Int? = null
        var int: Int? = null
        var long: Long? = null
        var float: Float? = null
        var double: Double? = null
        var string: IrDataIndex? = null
        var oneOfCase: IrConst.ValueCase = IrConst.ValueCase.VALUE_NOT_SET
        while (hasData) {
            when (val fieldHeader = readInt32()) {
                8 -> {
                    null_ = readBool()
                    oneOfCase = IrConst.ValueCase.NULL
                }
                16 -> {
                    boolean = readBool()
                    oneOfCase = IrConst.ValueCase.BOOLEAN
                }
                24 -> {
                    char = readInt32()
                    oneOfCase = IrConst.ValueCase.CHAR
                }
                32 -> {
                    byte = readInt32()
                    oneOfCase = IrConst.ValueCase.BYTE
                }
                40 -> {
                    short = readInt32()
                    oneOfCase = IrConst.ValueCase.SHORT
                }
                48 -> {
                    int = readInt32()
                    oneOfCase = IrConst.ValueCase.INT
                }
                56 -> {
                    long = readInt64()
                    oneOfCase = IrConst.ValueCase.LONG
                }
                69 -> {
                    float = readFloat()
                    oneOfCase = IrConst.ValueCase.FLOAT
                }
                73 -> {
                    double = readDouble()
                    oneOfCase = IrConst.ValueCase.DOUBLE
                }
                82 -> {
                    string = readWithLength { readIrDataIndex() }
                    oneOfCase = IrConst.ValueCase.STRING
                }
                else -> skip(fieldHeader and 7)
            }
        }
        return IrConst(oneOfCase!!, null_, boolean, char, byte, short, int, long, float, double, string)
    }

    fun readIrContinue(): IrContinue {
        var loopId: Int = 0
        var label: IrDataIndex? = null
        while (hasData) {
            when (val fieldHeader = readInt32()) {
                8 -> loopId = readInt32()
                18 -> label = readWithLength { readIrDataIndex() }
                else -> skip(fieldHeader and 7)
            }
        }
        return IrContinue(loopId, label)
    }

    fun readIrDelegatingConstructorCall(): IrDelegatingConstructorCall {
        var symbol: IrDataIndex? = null
        var memberAccess: MemberAccessCommon? = null
        while (hasData) {
            when (val fieldHeader = readInt32()) {
                10 -> symbol = readWithLength { readIrDataIndex() }
                18 -> memberAccess = readWithLength { readMemberAccessCommon() }
                else -> skip(fieldHeader and 7)
            }
        }
        return IrDelegatingConstructorCall(symbol!!, memberAccess!!)
    }

    fun readIrDoWhile(): IrDoWhile {
        var loop: Loop? = null
        while (hasData) {
            when (val fieldHeader = readInt32()) {
                10 -> loop = readWithLength { readLoop() }
                else -> skip(fieldHeader and 7)
            }
        }
        return IrDoWhile(loop!!)
    }

    fun readIrEnumConstructorCall(): IrEnumConstructorCall {
        var symbol: IrDataIndex? = null
        var memberAccess: MemberAccessCommon? = null
        while (hasData) {
            when (val fieldHeader = readInt32()) {
                10 -> symbol = readWithLength { readIrDataIndex() }
                18 -> memberAccess = readWithLength { readMemberAccessCommon() }
                else -> skip(fieldHeader and 7)
            }
        }
        return IrEnumConstructorCall(symbol!!, memberAccess!!)
    }

    fun readIrGetClass(): IrGetClass {
        var argument: IrExpression? = null
        while (hasData) {
            when (val fieldHeader = readInt32()) {
                10 -> argument = readWithLength { readIrExpression() }
                else -> skip(fieldHeader and 7)
            }
        }
        return IrGetClass(argument!!)
    }

    fun readIrGetEnumValue(): IrGetEnumValue {
        var symbol: IrDataIndex? = null
        while (hasData) {
            when (val fieldHeader = readInt32()) {
                18 -> symbol = readWithLength { readIrDataIndex() }
                else -> skip(fieldHeader and 7)
            }
        }
        return IrGetEnumValue(symbol!!)
    }

    fun readFieldAccessCommon(): FieldAccessCommon {
        var symbol: IrDataIndex? = null
        var super_: IrDataIndex? = null
        var receiver: IrExpression? = null
        while (hasData) {
            when (val fieldHeader = readInt32()) {
                10 -> symbol = readWithLength { readIrDataIndex() }
                18 -> super_ = readWithLength { readIrDataIndex() }
                26 -> receiver = readWithLength { readIrExpression() }
                else -> skip(fieldHeader and 7)
            }
        }
        return FieldAccessCommon(symbol!!, super_, receiver)
    }

    fun readIrGetField(): IrGetField {
        var fieldAccess: FieldAccessCommon? = null
        var origin: IrStatementOrigin? = null
        while (hasData) {
            when (val fieldHeader = readInt32()) {
                10 -> fieldAccess = readWithLength { readFieldAccessCommon() }
                18 -> origin = readWithLength { readIrStatementOrigin() }
                else -> skip(fieldHeader and 7)
            }
        }
        return IrGetField(fieldAccess!!, origin)
    }

    fun readIrGetValue(): IrGetValue {
        var symbol: IrDataIndex? = null
        var origin: IrStatementOrigin? = null
        while (hasData) {
            when (val fieldHeader = readInt32()) {
                10 -> symbol = readWithLength { readIrDataIndex() }
                18 -> origin = readWithLength { readIrStatementOrigin() }
                else -> skip(fieldHeader and 7)
            }
        }
        return IrGetValue(symbol!!, origin)
    }

    fun readIrGetObject(): IrGetObject {
        var symbol: IrDataIndex? = null
        while (hasData) {
            when (val fieldHeader = readInt32()) {
                10 -> symbol = readWithLength { readIrDataIndex() }
                else -> skip(fieldHeader and 7)
            }
        }
        return IrGetObject(symbol!!)
    }

    fun readIrInstanceInitializerCall(): IrInstanceInitializerCall {
        var symbol: IrDataIndex? = null
        while (hasData) {
            when (val fieldHeader = readInt32()) {
                10 -> symbol = readWithLength { readIrDataIndex() }
                else -> skip(fieldHeader and 7)
            }
        }
        return IrInstanceInitializerCall(symbol!!)
    }

    fun readLoop(): Loop {
        var loopId: Int = 0
        var condition: IrExpression? = null
        var label: IrDataIndex? = null
        var body: IrExpression? = null
        var origin: IrStatementOrigin? = null
        while (hasData) {
            when (val fieldHeader = readInt32()) {
                8 -> loopId = readInt32()
                18 -> condition = readWithLength { readIrExpression() }
                26 -> label = readWithLength { readIrDataIndex() }
                34 -> body = readWithLength { readIrExpression() }
                42 -> origin = readWithLength { readIrStatementOrigin() }
                else -> skip(fieldHeader and 7)
            }
        }
        return Loop(loopId, condition!!, label, body, origin)
    }

    fun readIrReturn(): IrReturn {
        var returnTarget: IrDataIndex? = null
        var value: IrExpression? = null
        while (hasData) {
            when (val fieldHeader = readInt32()) {
                10 -> returnTarget = readWithLength { readIrDataIndex() }
                18 -> value = readWithLength { readIrExpression() }
                else -> skip(fieldHeader and 7)
            }
        }
        return IrReturn(returnTarget!!, value!!)
    }

    fun readIrSetField(): IrSetField {
        var fieldAccess: FieldAccessCommon? = null
        var value: IrExpression? = null
        var origin: IrStatementOrigin? = null
        while (hasData) {
            when (val fieldHeader = readInt32()) {
                10 -> fieldAccess = readWithLength { readFieldAccessCommon() }
                18 -> value = readWithLength { readIrExpression() }
                26 -> origin = readWithLength { readIrStatementOrigin() }
                else -> skip(fieldHeader and 7)
            }
        }
        return IrSetField(fieldAccess!!, value!!, origin)
    }

    fun readIrSetVariable(): IrSetVariable {
        var symbol: IrDataIndex? = null
        var value: IrExpression? = null
        var origin: IrStatementOrigin? = null
        while (hasData) {
            when (val fieldHeader = readInt32()) {
                10 -> symbol = readWithLength { readIrDataIndex() }
                18 -> value = readWithLength { readIrExpression() }
                26 -> origin = readWithLength { readIrStatementOrigin() }
                else -> skip(fieldHeader and 7)
            }
        }
        return IrSetVariable(symbol!!, value!!, origin)
    }

    fun readIrSpreadElement(): IrSpreadElement {
        var expression: IrExpression? = null
        var coordinates: Coordinates? = null
        while (hasData) {
            when (val fieldHeader = readInt32()) {
                10 -> expression = readWithLength { readIrExpression() }
                18 -> coordinates = readWithLength { readCoordinates() }
                else -> skip(fieldHeader and 7)
            }
        }
        return IrSpreadElement(expression!!, coordinates!!)
    }

    fun readIrStringConcat(): IrStringConcat {
        var argument: MutableList<IrExpression> = mutableListOf()
        while (hasData) {
            when (val fieldHeader = readInt32()) {
                10 -> argument.add(readWithLength { readIrExpression() })
                else -> skip(fieldHeader and 7)
            }
        }
        return IrStringConcat(argument)
    }

    fun readIrThrow(): IrThrow {
        var value: IrExpression? = null
        while (hasData) {
            when (val fieldHeader = readInt32()) {
                10 -> value = readWithLength { readIrExpression() }
                else -> skip(fieldHeader and 7)
            }
        }
        return IrThrow(value!!)
    }

    fun readIrTry(): IrTry {
        var result: IrExpression? = null
        var catch: MutableList<IrStatement> = mutableListOf()
        var finally: IrExpression? = null
        while (hasData) {
            when (val fieldHeader = readInt32()) {
                10 -> result = readWithLength { readIrExpression() }
                18 -> catch.add(readWithLength { readIrStatement() })
                26 -> finally = readWithLength { readIrExpression() }
                else -> skip(fieldHeader and 7)
            }
        }
        return IrTry(result!!, catch, finally)
    }

    fun readIrTypeOp(): IrTypeOp {
        var operator: IrTypeOperator? = null
        var operand: IrDataIndex? = null
        var argument: IrExpression? = null
        while (hasData) {
            when (val fieldHeader = readInt32()) {
                8 -> operator = IrTypeOperator.fromIndex(readInt32())
                18 -> operand = readWithLength { readIrDataIndex() }
                26 -> argument = readWithLength { readIrExpression() }
                else -> skip(fieldHeader and 7)
            }
        }
        return IrTypeOp(operator!!, operand!!, argument!!)
    }

    fun readIrVararg(): IrVararg {
        var elementType: IrDataIndex? = null
        var element: MutableList<IrVarargElement> = mutableListOf()
        while (hasData) {
            when (val fieldHeader = readInt32()) {
                10 -> elementType = readWithLength { readIrDataIndex() }
                18 -> element.add(readWithLength { readIrVarargElement() })
                else -> skip(fieldHeader and 7)
            }
        }
        return IrVararg(elementType!!, element)
    }

    fun readIrVarargElement(): IrVarargElement {
        var expression: IrExpression? = null
        var spreadElement: IrSpreadElement? = null
        var oneOfCase: IrVarargElement.VarargElementCase = IrVarargElement.VarargElementCase.VARARG_ELEMENT_NOT_SET
        while (hasData) {
            when (val fieldHeader = readInt32()) {
                10 -> {
                    expression = readWithLength { readIrExpression() }
                    oneOfCase = IrVarargElement.VarargElementCase.EXPRESSION
                }
                18 -> {
                    spreadElement = readWithLength { readIrSpreadElement() }
                    oneOfCase = IrVarargElement.VarargElementCase.SPREAD_ELEMENT
                }
                else -> skip(fieldHeader and 7)
            }
        }
        return IrVarargElement(oneOfCase!!, expression, spreadElement)
    }

    fun readIrWhen(): IrWhen {
        var branch: MutableList<IrStatement> = mutableListOf()
        var origin: IrStatementOrigin? = null
        while (hasData) {
            when (val fieldHeader = readInt32()) {
                10 -> branch.add(readWithLength { readIrStatement() })
                18 -> origin = readWithLength { readIrStatementOrigin() }
                else -> skip(fieldHeader and 7)
            }
        }
        return IrWhen(branch, origin)
    }

    fun readIrWhile(): IrWhile {
        var loop: Loop? = null
        while (hasData) {
            when (val fieldHeader = readInt32()) {
                10 -> loop = readWithLength { readLoop() }
                else -> skip(fieldHeader and 7)
            }
        }
        return IrWhile(loop!!)
    }

    fun readIrFunctionExpression(): IrFunctionExpression {
        var function: IrFunction? = null
        var origin: IrStatementOrigin? = null
        while (hasData) {
            when (val fieldHeader = readInt32()) {
                10 -> function = readWithLength { readIrFunction() }
                18 -> origin = readWithLength { readIrStatementOrigin() }
                else -> skip(fieldHeader and 7)
            }
        }
        return IrFunctionExpression(function!!, origin!!)
    }

    fun readIrDynamicMemberExpression(): IrDynamicMemberExpression {
        var memberName: IrDataIndex? = null
        var receiver: IrExpression? = null
        while (hasData) {
            when (val fieldHeader = readInt32()) {
                10 -> memberName = readWithLength { readIrDataIndex() }
                18 -> receiver = readWithLength { readIrExpression() }
                else -> skip(fieldHeader and 7)
            }
        }
        return IrDynamicMemberExpression(memberName!!, receiver!!)
    }

    fun readIrDynamicOperatorExpression(): IrDynamicOperatorExpression {
        var operator: IrDynamicOperatorExpression.IrDynamicOperator? = null
        var receiver: IrExpression? = null
        var argument: MutableList<IrExpression> = mutableListOf()
        while (hasData) {
            when (val fieldHeader = readInt32()) {
                8 -> operator = IrDynamicOperatorExpression.IrDynamicOperator.fromIndex(readInt32())
                18 -> receiver = readWithLength { readIrExpression() }
                26 -> argument.add(readWithLength { readIrExpression() })
                else -> skip(fieldHeader and 7)
            }
        }
        return IrDynamicOperatorExpression(operator!!, receiver!!, argument)
    }

    fun readIrOperation(): IrOperation {
        var block: IrBlock? = null
        var break_: IrBreak? = null
        var call: IrCall? = null
        var classReference: IrClassReference? = null
        var composite: IrComposite? = null
        var const: IrConst? = null
        var continue_: IrContinue? = null
        var delegatingConstructorCall: IrDelegatingConstructorCall? = null
        var doWhile: IrDoWhile? = null
        var enumConstructorCall: IrEnumConstructorCall? = null
        var functionReference: IrFunctionReference? = null
        var getClass: IrGetClass? = null
        var getEnumValue: IrGetEnumValue? = null
        var getField: IrGetField? = null
        var getObject: IrGetObject? = null
        var getValue: IrGetValue? = null
        var instanceInitializerCall: IrInstanceInitializerCall? = null
        var propertyReference: IrPropertyReference? = null
        var return_: IrReturn? = null
        var setField: IrSetField? = null
        var setVariable: IrSetVariable? = null
        var stringConcat: IrStringConcat? = null
        var throw_: IrThrow? = null
        var try_: IrTry? = null
        var typeOp: IrTypeOp? = null
        var vararg: IrVararg? = null
        var when_: IrWhen? = null
        var while_: IrWhile? = null
        var dynamicMember: IrDynamicMemberExpression? = null
        var dynamicOperator: IrDynamicOperatorExpression? = null
        var localDelegatedPropertyReference: IrLocalDelegatedPropertyReference? = null
        var constructorCall: IrConstructorCall? = null
        var functionExpression: IrFunctionExpression? = null
        var oneOfCase: IrOperation.OperationCase = IrOperation.OperationCase.OPERATION_NOT_SET
        while (hasData) {
            when (val fieldHeader = readInt32()) {
                10 -> {
                    block = readWithLength { readIrBlock() }
                    oneOfCase = IrOperation.OperationCase.BLOCK
                }
                18 -> {
                    break_ = readWithLength { readIrBreak() }
                    oneOfCase = IrOperation.OperationCase.BREAK
                }
                26 -> {
                    call = readWithLength { readIrCall() }
                    oneOfCase = IrOperation.OperationCase.CALL
                }
                34 -> {
                    classReference = readWithLength { readIrClassReference() }
                    oneOfCase = IrOperation.OperationCase.CLASS_REFERENCE
                }
                42 -> {
                    composite = readWithLength { readIrComposite() }
                    oneOfCase = IrOperation.OperationCase.COMPOSITE
                }
                50 -> {
                    const = readWithLength { readIrConst() }
                    oneOfCase = IrOperation.OperationCase.CONST
                }
                58 -> {
                    continue_ = readWithLength { readIrContinue() }
                    oneOfCase = IrOperation.OperationCase.CONTINUE
                }
                66 -> {
                    delegatingConstructorCall = readWithLength { readIrDelegatingConstructorCall() }
                    oneOfCase = IrOperation.OperationCase.DELEGATING_CONSTRUCTOR_CALL
                }
                74 -> {
                    doWhile = readWithLength { readIrDoWhile() }
                    oneOfCase = IrOperation.OperationCase.DO_WHILE
                }
                82 -> {
                    enumConstructorCall = readWithLength { readIrEnumConstructorCall() }
                    oneOfCase = IrOperation.OperationCase.ENUM_CONSTRUCTOR_CALL
                }
                90 -> {
                    functionReference = readWithLength { readIrFunctionReference() }
                    oneOfCase = IrOperation.OperationCase.FUNCTION_REFERENCE
                }
                98 -> {
                    getClass = readWithLength { readIrGetClass() }
                    oneOfCase = IrOperation.OperationCase.GET_CLASS
                }
                106 -> {
                    getEnumValue = readWithLength { readIrGetEnumValue() }
                    oneOfCase = IrOperation.OperationCase.GET_ENUM_VALUE
                }
                114 -> {
                    getField = readWithLength { readIrGetField() }
                    oneOfCase = IrOperation.OperationCase.GET_FIELD
                }
                122 -> {
                    getObject = readWithLength { readIrGetObject() }
                    oneOfCase = IrOperation.OperationCase.GET_OBJECT
                }
                130 -> {
                    getValue = readWithLength { readIrGetValue() }
                    oneOfCase = IrOperation.OperationCase.GET_VALUE
                }
                138 -> {
                    instanceInitializerCall = readWithLength { readIrInstanceInitializerCall() }
                    oneOfCase = IrOperation.OperationCase.INSTANCE_INITIALIZER_CALL
                }
                146 -> {
                    propertyReference = readWithLength { readIrPropertyReference() }
                    oneOfCase = IrOperation.OperationCase.PROPERTY_REFERENCE
                }
                154 -> {
                    return_ = readWithLength { readIrReturn() }
                    oneOfCase = IrOperation.OperationCase.RETURN
                }
                162 -> {
                    setField = readWithLength { readIrSetField() }
                    oneOfCase = IrOperation.OperationCase.SET_FIELD
                }
                170 -> {
                    setVariable = readWithLength { readIrSetVariable() }
                    oneOfCase = IrOperation.OperationCase.SET_VARIABLE
                }
                178 -> {
                    stringConcat = readWithLength { readIrStringConcat() }
                    oneOfCase = IrOperation.OperationCase.STRING_CONCAT
                }
                186 -> {
                    throw_ = readWithLength { readIrThrow() }
                    oneOfCase = IrOperation.OperationCase.THROW
                }
                194 -> {
                    try_ = readWithLength { readIrTry() }
                    oneOfCase = IrOperation.OperationCase.TRY
                }
                202 -> {
                    typeOp = readWithLength { readIrTypeOp() }
                    oneOfCase = IrOperation.OperationCase.TYPE_OP
                }
                210 -> {
                    vararg = readWithLength { readIrVararg() }
                    oneOfCase = IrOperation.OperationCase.VARARG
                }
                218 -> {
                    when_ = readWithLength { readIrWhen() }
                    oneOfCase = IrOperation.OperationCase.WHEN
                }
                226 -> {
                    while_ = readWithLength { readIrWhile() }
                    oneOfCase = IrOperation.OperationCase.WHILE
                }
                234 -> {
                    dynamicMember = readWithLength { readIrDynamicMemberExpression() }
                    oneOfCase = IrOperation.OperationCase.DYNAMIC_MEMBER
                }
                242 -> {
                    dynamicOperator = readWithLength { readIrDynamicOperatorExpression() }
                    oneOfCase = IrOperation.OperationCase.DYNAMIC_OPERATOR
                }
                250 -> {
                    localDelegatedPropertyReference = readWithLength { readIrLocalDelegatedPropertyReference() }
                    oneOfCase = IrOperation.OperationCase.LOCAL_DELEGATED_PROPERTY_REFERENCE
                }
                258 -> {
                    constructorCall = readWithLength { readIrConstructorCall() }
                    oneOfCase = IrOperation.OperationCase.CONSTRUCTOR_CALL
                }
                266 -> {
                    functionExpression = readWithLength { readIrFunctionExpression() }
                    oneOfCase = IrOperation.OperationCase.FUNCTION_EXPRESSION
                }
                else -> skip(fieldHeader and 7)
            }
        }
        return IrOperation(oneOfCase!!, block, break_, call, classReference, composite, const, continue_, delegatingConstructorCall, doWhile, enumConstructorCall, functionReference, getClass, getEnumValue, getField, getObject, getValue, instanceInitializerCall, propertyReference, return_, setField, setVariable, stringConcat, throw_, try_, typeOp, vararg, when_, while_, dynamicMember, dynamicOperator, localDelegatedPropertyReference, constructorCall, functionExpression)
    }

    fun readIrExpression(): IrExpression {
        var operation: IrOperation? = null
        var type: IrDataIndex? = null
        var coordinates: Coordinates? = null
        while (hasData) {
            when (val fieldHeader = readInt32()) {
                10 -> operation = readWithLength { readIrOperation() }
                18 -> type = readWithLength { readIrDataIndex() }
                26 -> coordinates = readWithLength { readCoordinates() }
                else -> skip(fieldHeader and 7)
            }
        }
        return IrExpression(operation!!, type!!, coordinates!!)
    }

    fun readNullableIrExpression(): NullableIrExpression {
        var expression: IrExpression? = null
        while (hasData) {
            when (val fieldHeader = readInt32()) {
                10 -> expression = readWithLength { readIrExpression() }
                else -> skip(fieldHeader and 7)
            }
        }
        return NullableIrExpression(expression)
    }

    fun readIrDeclarationBase(): IrDeclarationBase {
        var symbol: IrDataIndex? = null
        var origin: IrDeclarationOrigin? = null
        var coordinates: Coordinates? = null
        var annotations: Annotations? = null
        while (hasData) {
            when (val fieldHeader = readInt32()) {
                10 -> symbol = readWithLength { readIrDataIndex() }
                18 -> origin = readWithLength { readIrDeclarationOrigin() }
                26 -> coordinates = readWithLength { readCoordinates() }
                34 -> annotations = readWithLength { readAnnotations() }
                else -> skip(fieldHeader and 7)
            }
        }
        return IrDeclarationBase(symbol!!, origin!!, coordinates!!, annotations!!)
    }

    fun readIrFunctionBase(): IrFunctionBase {
        var base: IrDeclarationBase? = null
        var name: IrDataIndex? = null
        var visibility: Visibility? = null
        var isInline: Boolean = false
        var isExternal: Boolean = false
        var typeParameters: IrTypeParameterContainer? = null
        var dispatchReceiver: IrValueParameter? = null
        var extensionReceiver: IrValueParameter? = null
        var valueParameter: MutableList<IrValueParameter> = mutableListOf()
        var body: IrDataIndex? = null
        var returnType: IrDataIndex? = null
        while (hasData) {
            when (val fieldHeader = readInt32()) {
                10 -> base = readWithLength { readIrDeclarationBase() }
                18 -> name = readWithLength { readIrDataIndex() }
                26 -> visibility = readWithLength { readVisibility() }
                32 -> isInline = readBool()
                40 -> isExternal = readBool()
                50 -> typeParameters = readWithLength { readIrTypeParameterContainer() }
                58 -> dispatchReceiver = readWithLength { readIrValueParameter() }
                66 -> extensionReceiver = readWithLength { readIrValueParameter() }
                74 -> valueParameter.add(readWithLength { readIrValueParameter() })
                82 -> body = readWithLength { readIrDataIndex() }
                90 -> returnType = readWithLength { readIrDataIndex() }
                else -> skip(fieldHeader and 7)
            }
        }
        return IrFunctionBase(base!!, name!!, visibility!!, isInline, isExternal, typeParameters!!, dispatchReceiver, extensionReceiver, valueParameter, body, returnType!!)
    }

    fun readIrFunction(): IrFunction {
        var base: IrFunctionBase? = null
        var modality: ModalityKind? = null
        var isTailrec: Boolean = false
        var isSuspend: Boolean = false
        var overridden: MutableList<IrDataIndex> = mutableListOf()
        while (hasData) {
            when (val fieldHeader = readInt32()) {
                10 -> base = readWithLength { readIrFunctionBase() }
                16 -> modality = ModalityKind.fromIndex(readInt32())
                24 -> isTailrec = readBool()
                32 -> isSuspend = readBool()
                42 -> overridden.add(readWithLength { readIrDataIndex() })
                else -> skip(fieldHeader and 7)
            }
        }
        return IrFunction(base!!, modality!!, isTailrec, isSuspend, overridden)
    }

    fun readIrConstructor(): IrConstructor {
        var base: IrFunctionBase? = null
        var isPrimary: Boolean = false
        while (hasData) {
            when (val fieldHeader = readInt32()) {
                10 -> base = readWithLength { readIrFunctionBase() }
                16 -> isPrimary = readBool()
                else -> skip(fieldHeader and 7)
            }
        }
        return IrConstructor(base!!, isPrimary)
    }

    fun readIrField(): IrField {
        var base: IrDeclarationBase? = null
        var initializer: IrDataIndex? = null
        var name: IrDataIndex? = null
        var visibility: Visibility? = null
        var isFinal: Boolean = false
        var isExternal: Boolean = false
        var isStatic: Boolean = false
        var type: IrDataIndex? = null
        while (hasData) {
            when (val fieldHeader = readInt32()) {
                10 -> base = readWithLength { readIrDeclarationBase() }
                18 -> initializer = readWithLength { readIrDataIndex() }
                26 -> name = readWithLength { readIrDataIndex() }
                34 -> visibility = readWithLength { readVisibility() }
                40 -> isFinal = readBool()
                48 -> isExternal = readBool()
                56 -> isStatic = readBool()
                66 -> type = readWithLength { readIrDataIndex() }
                else -> skip(fieldHeader and 7)
            }
        }
        return IrField(base!!, initializer, name!!, visibility!!, isFinal, isExternal, isStatic, type!!)
    }

    fun readIrLocalDelegatedProperty(): IrLocalDelegatedProperty {
        var base: IrDeclarationBase? = null
        var name: IrDataIndex? = null
        var type: IrDataIndex? = null
        var isVar: Boolean = false
        var delegate: IrVariable? = null
        var getter: IrFunction? = null
        var setter: IrFunction? = null
        while (hasData) {
            when (val fieldHeader = readInt32()) {
                10 -> base = readWithLength { readIrDeclarationBase() }
                18 -> name = readWithLength { readIrDataIndex() }
                26 -> type = readWithLength { readIrDataIndex() }
                32 -> isVar = readBool()
                42 -> delegate = readWithLength { readIrVariable() }
                50 -> getter = readWithLength { readIrFunction() }
                58 -> setter = readWithLength { readIrFunction() }
                else -> skip(fieldHeader and 7)
            }
        }
        return IrLocalDelegatedProperty(base!!, name!!, type!!, isVar, delegate!!, getter, setter)
    }

    fun readIrProperty(): IrProperty {
        var base: IrDeclarationBase? = null
        var name: IrDataIndex? = null
        var visibility: Visibility? = null
        var modality: ModalityKind? = null
        var isVar: Boolean = false
        var isConst: Boolean = false
        var isLateinit: Boolean = false
        var isDelegated: Boolean = false
        var isExternal: Boolean = false
        var backingField: IrField? = null
        var getter: IrFunction? = null
        var setter: IrFunction? = null
        while (hasData) {
            when (val fieldHeader = readInt32()) {
                10 -> base = readWithLength { readIrDeclarationBase() }
                18 -> name = readWithLength { readIrDataIndex() }
                26 -> visibility = readWithLength { readVisibility() }
                32 -> modality = ModalityKind.fromIndex(readInt32())
                40 -> isVar = readBool()
                48 -> isConst = readBool()
                56 -> isLateinit = readBool()
                64 -> isDelegated = readBool()
                72 -> isExternal = readBool()
                82 -> backingField = readWithLength { readIrField() }
                90 -> getter = readWithLength { readIrFunction() }
                98 -> setter = readWithLength { readIrFunction() }
                else -> skip(fieldHeader and 7)
            }
        }
        return IrProperty(base!!, name!!, visibility!!, modality!!, isVar, isConst, isLateinit, isDelegated, isExternal, backingField, getter, setter)
    }

    fun readIrVariable(): IrVariable {
        var base: IrDeclarationBase? = null
        var name: IrDataIndex? = null
        var type: IrDataIndex? = null
        var isVar: Boolean = false
        var isConst: Boolean = false
        var isLateinit: Boolean = false
        var initializer: IrExpression? = null
        while (hasData) {
            when (val fieldHeader = readInt32()) {
                10 -> base = readWithLength { readIrDeclarationBase() }
                18 -> name = readWithLength { readIrDataIndex() }
                26 -> type = readWithLength { readIrDataIndex() }
                32 -> isVar = readBool()
                40 -> isConst = readBool()
                48 -> isLateinit = readBool()
                58 -> initializer = readWithLength { readIrExpression() }
                else -> skip(fieldHeader and 7)
            }
        }
        return IrVariable(base!!, name!!, type!!, isVar, isConst, isLateinit, initializer)
    }

    fun readIrValueParameter(): IrValueParameter {
        var base: IrDeclarationBase? = null
        var name: IrDataIndex? = null
        var index: Int = 0
        var type: IrDataIndex? = null
        var varargElementType: IrDataIndex? = null
        var isCrossinline: Boolean = false
        var isNoinline: Boolean = false
        var defaultValue: IrDataIndex? = null
        while (hasData) {
            when (val fieldHeader = readInt32()) {
                10 -> base = readWithLength { readIrDeclarationBase() }
                18 -> name = readWithLength { readIrDataIndex() }
                24 -> index = readInt32()
                34 -> type = readWithLength { readIrDataIndex() }
                42 -> varargElementType = readWithLength { readIrDataIndex() }
                48 -> isCrossinline = readBool()
                56 -> isNoinline = readBool()
                66 -> defaultValue = readWithLength { readIrDataIndex() }
                else -> skip(fieldHeader and 7)
            }
        }
        return IrValueParameter(base!!, name!!, index, type!!, varargElementType, isCrossinline, isNoinline, defaultValue)
    }

    fun readIrTypeParameter(): IrTypeParameter {
        var base: IrDeclarationBase? = null
        var name: IrDataIndex? = null
        var index: Int = 0
        var variance: IrTypeVariance? = null
        var superType: MutableList<IrDataIndex> = mutableListOf()
        var isReified: Boolean = false
        while (hasData) {
            when (val fieldHeader = readInt32()) {
                10 -> base = readWithLength { readIrDeclarationBase() }
                18 -> name = readWithLength { readIrDataIndex() }
                24 -> index = readInt32()
                32 -> variance = IrTypeVariance.fromIndex(readInt32())
                42 -> superType.add(readWithLength { readIrDataIndex() })
                48 -> isReified = readBool()
                else -> skip(fieldHeader and 7)
            }
        }
        return IrTypeParameter(base!!, name!!, index, variance!!, superType, isReified)
    }

    fun readIrTypeParameterContainer(): IrTypeParameterContainer {
        var typeParameter: MutableList<IrTypeParameter> = mutableListOf()
        while (hasData) {
            when (val fieldHeader = readInt32()) {
                10 -> typeParameter.add(readWithLength { readIrTypeParameter() })
                else -> skip(fieldHeader and 7)
            }
        }
        return IrTypeParameterContainer(typeParameter)
    }

    fun readIrClass(): IrClass {
        var base: IrDeclarationBase? = null
        var name: IrDataIndex? = null
        var kind: ClassKind? = null
        var visibility: Visibility? = null
        var modality: ModalityKind? = null
        var isCompanion: Boolean = false
        var isInner: Boolean = false
        var isData: Boolean = false
        var isExternal: Boolean = false
        var isInline: Boolean = false
        var thisReceiver: IrValueParameter? = null
        var typeParameters: IrTypeParameterContainer? = null
        var declarationContainer: IrDeclarationContainer? = null
        var superType: MutableList<IrDataIndex> = mutableListOf()
        while (hasData) {
            when (val fieldHeader = readInt32()) {
                10 -> base = readWithLength { readIrDeclarationBase() }
                18 -> name = readWithLength { readIrDataIndex() }
                24 -> kind = ClassKind.fromIndex(readInt32())
                34 -> visibility = readWithLength { readVisibility() }
                40 -> modality = ModalityKind.fromIndex(readInt32())
                48 -> isCompanion = readBool()
                56 -> isInner = readBool()
                64 -> isData = readBool()
                72 -> isExternal = readBool()
                80 -> isInline = readBool()
                90 -> thisReceiver = readWithLength { readIrValueParameter() }
                98 -> typeParameters = readWithLength { readIrTypeParameterContainer() }
                106 -> declarationContainer = readWithLength { readIrDeclarationContainer() }
                114 -> superType.add(readWithLength { readIrDataIndex() })
                else -> skip(fieldHeader and 7)
            }
        }
        return IrClass(base!!, name!!, kind!!, visibility!!, modality!!, isCompanion, isInner, isData, isExternal, isInline, thisReceiver, typeParameters!!, declarationContainer!!, superType)
    }

    fun readIrTypeAlias(): IrTypeAlias {
        var base: IrDeclarationBase? = null
        var name: IrDataIndex? = null
        var visibility: Visibility? = null
        var typeParameters: IrTypeParameterContainer? = null
        var expandedType: IrDataIndex? = null
        var isActual: Boolean = false
        while (hasData) {
            when (val fieldHeader = readInt32()) {
                10 -> base = readWithLength { readIrDeclarationBase() }
                18 -> name = readWithLength { readIrDataIndex() }
                26 -> visibility = readWithLength { readVisibility() }
                34 -> typeParameters = readWithLength { readIrTypeParameterContainer() }
                42 -> expandedType = readWithLength { readIrDataIndex() }
                48 -> isActual = readBool()
                else -> skip(fieldHeader and 7)
            }
        }
        return IrTypeAlias(base!!, name!!, visibility!!, typeParameters!!, expandedType!!, isActual)
    }

    fun readIrEnumEntry(): IrEnumEntry {
        var base: IrDeclarationBase? = null
        var initializer: IrDataIndex? = null
        var correspondingClass: IrClass? = null
        var name: IrDataIndex? = null
        while (hasData) {
            when (val fieldHeader = readInt32()) {
                10 -> base = readWithLength { readIrDeclarationBase() }
                18 -> initializer = readWithLength { readIrDataIndex() }
                26 -> correspondingClass = readWithLength { readIrClass() }
                34 -> name = readWithLength { readIrDataIndex() }
                else -> skip(fieldHeader and 7)
            }
        }
        return IrEnumEntry(base!!, initializer, correspondingClass, name!!)
    }

    fun readIrAnonymousInit(): IrAnonymousInit {
        var base: IrDeclarationBase? = null
        var body: IrDataIndex? = null
        while (hasData) {
            when (val fieldHeader = readInt32()) {
                10 -> base = readWithLength { readIrDeclarationBase() }
                18 -> body = readWithLength { readIrDataIndex() }
                else -> skip(fieldHeader and 7)
            }
        }
        return IrAnonymousInit(base!!, body!!)
    }

    fun readIrDeclaration(): IrDeclaration {
        var irAnonymousInit: IrAnonymousInit? = null
        var irClass: IrClass? = null
        var irConstructor: IrConstructor? = null
        var irEnumEntry: IrEnumEntry? = null
        var irField: IrField? = null
        var irFunction: IrFunction? = null
        var irProperty: IrProperty? = null
        var irTypeParameter: IrTypeParameter? = null
        var irVariable: IrVariable? = null
        var irValueParameter: IrValueParameter? = null
        var irLocalDelegatedProperty: IrLocalDelegatedProperty? = null
        var irTypeAlias: IrTypeAlias? = null
        var oneOfCase: IrDeclaration.DeclaratorCase = IrDeclaration.DeclaratorCase.DECLARATOR_NOT_SET
        while (hasData) {
            when (val fieldHeader = readInt32()) {
                10 -> {
                    irAnonymousInit = readWithLength { readIrAnonymousInit() }
                    oneOfCase = IrDeclaration.DeclaratorCase.IR_ANONYMOUS_INIT
                }
                18 -> {
                    irClass = readWithLength { readIrClass() }
                    oneOfCase = IrDeclaration.DeclaratorCase.IR_CLASS
                }
                26 -> {
                    irConstructor = readWithLength { readIrConstructor() }
                    oneOfCase = IrDeclaration.DeclaratorCase.IR_CONSTRUCTOR
                }
                34 -> {
                    irEnumEntry = readWithLength { readIrEnumEntry() }
                    oneOfCase = IrDeclaration.DeclaratorCase.IR_ENUM_ENTRY
                }
                42 -> {
                    irField = readWithLength { readIrField() }
                    oneOfCase = IrDeclaration.DeclaratorCase.IR_FIELD
                }
                50 -> {
                    irFunction = readWithLength { readIrFunction() }
                    oneOfCase = IrDeclaration.DeclaratorCase.IR_FUNCTION
                }
                58 -> {
                    irProperty = readWithLength { readIrProperty() }
                    oneOfCase = IrDeclaration.DeclaratorCase.IR_PROPERTY
                }
                66 -> {
                    irTypeParameter = readWithLength { readIrTypeParameter() }
                    oneOfCase = IrDeclaration.DeclaratorCase.IR_TYPE_PARAMETER
                }
                74 -> {
                    irVariable = readWithLength { readIrVariable() }
                    oneOfCase = IrDeclaration.DeclaratorCase.IR_VARIABLE
                }
                82 -> {
                    irValueParameter = readWithLength { readIrValueParameter() }
                    oneOfCase = IrDeclaration.DeclaratorCase.IR_VALUE_PARAMETER
                }
                90 -> {
                    irLocalDelegatedProperty = readWithLength { readIrLocalDelegatedProperty() }
                    oneOfCase = IrDeclaration.DeclaratorCase.IR_LOCAL_DELEGATED_PROPERTY
                }
                98 -> {
                    irTypeAlias = readWithLength { readIrTypeAlias() }
                    oneOfCase = IrDeclaration.DeclaratorCase.IR_TYPE_ALIAS
                }
                else -> skip(fieldHeader and 7)
            }
        }
        return IrDeclaration(oneOfCase!!, irAnonymousInit, irClass, irConstructor, irEnumEntry, irField, irFunction, irProperty, irTypeParameter, irVariable, irValueParameter, irLocalDelegatedProperty, irTypeAlias)
    }

    fun readIrBranch(): IrBranch {
        var condition: IrExpression? = null
        var result: IrExpression? = null
        while (hasData) {
            when (val fieldHeader = readInt32()) {
                10 -> condition = readWithLength { readIrExpression() }
                18 -> result = readWithLength { readIrExpression() }
                else -> skip(fieldHeader and 7)
            }
        }
        return IrBranch(condition!!, result!!)
    }

    fun readIrBlockBody(): IrBlockBody {
        var statement: MutableList<IrStatement> = mutableListOf()
        while (hasData) {
            when (val fieldHeader = readInt32()) {
                10 -> statement.add(readWithLength { readIrStatement() })
                else -> skip(fieldHeader and 7)
            }
        }
        return IrBlockBody(statement)
    }

    fun readIrCatch(): IrCatch {
        var catchParameter: IrVariable? = null
        var result: IrExpression? = null
        while (hasData) {
            when (val fieldHeader = readInt32()) {
                10 -> catchParameter = readWithLength { readIrVariable() }
                18 -> result = readWithLength { readIrExpression() }
                else -> skip(fieldHeader and 7)
            }
        }
        return IrCatch(catchParameter!!, result!!)
    }

    fun readIrSyntheticBody(): IrSyntheticBody {
        var kind: IrSyntheticBodyKind? = null
        while (hasData) {
            when (val fieldHeader = readInt32()) {
                8 -> kind = IrSyntheticBodyKind.fromIndex(readInt32())
                else -> skip(fieldHeader and 7)
            }
        }
        return IrSyntheticBody(kind!!)
    }

    fun readIrStatement(): IrStatement {
        var coordinates: Coordinates? = null
        var declaration: IrDeclaration? = null
        var expression: IrExpression? = null
        var blockBody: IrBlockBody? = null
        var branch: IrBranch? = null
        var catch: IrCatch? = null
        var syntheticBody: IrSyntheticBody? = null
        var oneOfCase: IrStatement.StatementCase = IrStatement.StatementCase.STATEMENT_NOT_SET
        while (hasData) {
            when (val fieldHeader = readInt32()) {
                10 -> coordinates = readWithLength { readCoordinates() }
                18 -> {
                    declaration = readWithLength { readIrDeclaration() }
                    oneOfCase = IrStatement.StatementCase.DECLARATION
                }
                26 -> {
                    expression = readWithLength { readIrExpression() }
                    oneOfCase = IrStatement.StatementCase.EXPRESSION
                }
                34 -> {
                    blockBody = readWithLength { readIrBlockBody() }
                    oneOfCase = IrStatement.StatementCase.BLOCK_BODY
                }
                42 -> {
                    branch = readWithLength { readIrBranch() }
                    oneOfCase = IrStatement.StatementCase.BRANCH
                }
                50 -> {
                    catch = readWithLength { readIrCatch() }
                    oneOfCase = IrStatement.StatementCase.CATCH
                }
                58 -> {
                    syntheticBody = readWithLength { readIrSyntheticBody() }
                    oneOfCase = IrStatement.StatementCase.SYNTHETIC_BODY
                }
                else -> skip(fieldHeader and 7)
            }
        }
        return IrStatement(coordinates!!, oneOfCase!!, declaration, expression, blockBody, branch, catch, syntheticBody)
    }

}