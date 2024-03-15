// FIR_IDENTICAL

// OPT_IN: kotlin.js.ExperimentalJsExport
// OPT_IN: kotlin.ExperimentalUnsignedTypes
// DIAGNOSTICS: -FINAL_UPPER_BOUND, -UNUSED_PARAMETER
// DIAGNOSTICS: -INLINE_CLASS_IN_EXTERNAL_DECLARATION, -NON_EXPORTABLE_TYPE

external interface ExternalInterface
external open class ExternalOpenClass
external object ExternalObject


external fun correctJsInteropTypes(
    // primitive types
    boolean: Boolean,
    char: Char,
    byte: Byte,
    short: Short,
    int: Int,
    long: Long,
    float: Float,
    double: Double,
    // unsigned integer types
    uByte: UByte,
    uShort: UShort,
    uInt: UInt,
    uLong: ULong,
    // string types
    string: String,
    // function types
    function: () -> Unit,
    // external types
    externalInterface: ExternalInterface,
    externalOpenClass: ExternalOpenClass,
    externalObject: ExternalObject,
)

external fun correctNullableJsInteropTypes(
    // primitive types
    boolean: Boolean?,
    char: Char?,
    byte: Byte?,
    short: Short?,
    int: Int?,
    long: Long?,
    float: Float?,
    double: Double?,
    // unsigned integer types
    uByte: UByte?,
    uShort: UShort?,
    uInt: UInt?,
    uLong: ULong?,
    // string types
    string: String?,
    // function types
    function: (() -> Unit)?,
    // external types
    externalInterface: ExternalInterface?,
    externalOpenClass: ExternalOpenClass?,
    externalObject: ExternalObject?,
)


// correct JS interop types as return types

// Unit and Nothing
external fun unitAsReturnType(): Unit
external fun nothingAsReturnType(): Nothing

// primitive types
external fun booleanAsReturnType(): Boolean
external fun charAsReturnType(): Char
external fun byteAsReturnType(): Byte
external fun shortAsReturnType(): Short
external fun intAsReturnType(): Int
external fun longAsReturnType(): Long
external fun floatAsReturnType(): Float
external fun doubleAsReturnType(): Double

// unsigned integer types
external fun uByteAsReturnType(): UByte
external fun uShortAsReturnType(): UShort
external fun uIntAsReturnType(): UInt
external fun uLongAsReturnType(): ULong

// string types
external fun stringAsReturnType(): String

// function types
external fun functionAsReturnType(): () -> Unit

// external types
external fun externalInterfaceAsReturnType(): ExternalInterface
external fun externalOpenClassAsReturnType(): ExternalOpenClass
external fun externalObjectAsReturnType(): ExternalObject


// correct JS interop types as vararg parameter types

// primitive types
external fun booleanAsVarargParameterType(vararg args: Boolean)
external fun charAsVarargParameterType(vararg args: Char)
external fun byteAsVarargParameterType(vararg args: Byte)
external fun shortAsVarargParameterType(vararg args: Short)
external fun intAsVarargParameterType(vararg args: Int)
external fun longAsVarargParameterType(vararg args: Long)
external fun floatAsVarargParameterType(vararg args: Float)
external fun doubleAsVarargParameterType(vararg args: Double)

// unsigned integer types
external fun uByteAsVarargParameterType(vararg args: UByte)
external fun uShortAsVarargParameterType(vararg args: UShort)
external fun uIntAsVarargParameterType(vararg args: UInt)
external fun uLongAsVarargParameterType(vararg args: ULong)

// string types
external fun stringAsVarargParameterType(vararg args: String)

// function types
external fun functionAsVarargParameterType(vararg args: () -> Unit)

// external types
external fun externalInterfaceAsVarargParameterType(vararg args: ExternalInterface)
external fun externalOpenClassAsVarargParameterType(vararg args: ExternalOpenClass)
external fun externalObjectAsVarargParameterType(vararg args: ExternalObject)


external fun <
        // primitive types
        TBoolean: Boolean,
        TChar: Char,
        TByte: Byte,
        TShort: Short,
        TInt: Int,
        TLong: Long,
        TFloat: Float,
        TDouble: Double,
        // unsigned integer types
        TUByte: UByte,
        TUShort: UShort,
        TUInt: UInt,
        TULong: ULong,
        // string types
        TString: String,
        // function types
        TFunction: () -> Unit,
        // external types
        TExternalInterface: ExternalInterface,
        TExternalOpenClass: ExternalOpenClass,
        TExternalObject: ExternalObject,
        // correct type parameter
        TCorrectTypeParameter: TExternalInterface
> correctJsInteropTypesAsFunctionTypeParameterUpperBounds(
    boolean: TBoolean,
    char: TChar,
    byte: TByte,
    short: TShort,
    int: TInt,
    long: TLong,
    float: TFloat,
    double: TDouble,
    uByte: TUByte,
    uShort: TUShort,
    uInt: TUInt,
    uLong: TULong,
    string: TString,
    function: TFunction,
    externalInterface: TExternalInterface,
    externalOpenClass: TExternalOpenClass,
    externalObject: TExternalObject,
    correctTypeParameter: TCorrectTypeParameter
)

external class CorrectJsInteropTypesAsClassTypeParameterUpperBounds<
        // primitive types
        TBoolean: Boolean,
        TChar: Char,
        TByte: Byte,
        TShort: Short,
        TInt: Int,
        TLong: Long,
        TFloat: Float,
        TDouble: Double,
        // unsigned integer types
        TUByte: UByte,
        TUShort: UShort,
        TUInt: UInt,
        TULong: ULong,
        // string types
        TString: String,
        // function types
        TFunction: () -> Unit,
        // external types
        TExternalInterface: ExternalInterface,
        TExternalOpenClass: ExternalOpenClass,
        TExternalObject: ExternalObject,
        // correct type parameter
        TCorrectTypeParameter: TExternalInterface
>


// correct JS interop types as property types

// primitive types
external val booleanProperty: Boolean
external val charProperty: Char
external val byteProperty: Byte
external val shortProperty: Short
external val intProperty: Int
external val longProperty: Long
external val floatProperty: Float
external val doubleProperty: Double

// unsigned integer types
external val uByteProperty: UByte
external val uShortProperty: UShort
external val uIntProperty: UInt
external val uLongProperty: ULong

// string types
external val stringProperty: String

// function types
external val functionProperty: () -> Unit

// external types
external val externalInterfaceProperty: ExternalInterface
external val externalOpenClassProperty: ExternalOpenClass
external val externalObjectProperty: ExternalObject


external fun correctJsInteropTypesAsFunctionTypeParameterTypes(
    primitiveTypes: (Boolean, Char, Byte, Short, Int, Long, Float, Double) -> Unit,
    unsignedIntegerTypes: (UByte, UShort, UInt, ULong) -> Unit,
    string: (String) -> Unit,
    function: (() -> Unit) -> Unit,
    externalTypes: (ExternalInterface, ExternalOpenClass, ExternalObject) -> Unit,
)

external fun correctJsInteropTypesAsFunctionTypeReturnTypes(
    // Unit and Nothing
    unit: () -> Unit,
    nothing: () -> Nothing,
    // primitive types
    boolean: () -> Boolean,
    char: () -> Char,
    byte: () -> Byte,
    short: () -> Short,
    int: () -> Int,
    long: () -> Long,
    float: () -> Float,
    double: () -> Double,
    // unsigned integer types
    uByte: () -> UByte,
    uShort: () -> UShort,
    uInt: () -> UInt,
    uLong: () -> ULong,
    // string types
    string: () -> String,
    // function types
    function: () -> (() -> Unit),
    // external types
    externalInterface: () -> ExternalInterface,
    externalOpenClass: () -> ExternalOpenClass,
    externalObject: () -> ExternalObject,
)

fun correctJsInteropTypesInJsCodeFunction(
    // primitive types
    boolean: Boolean,
    char: Char,
    byte: Byte,
    short: Short,
    int: Int,
    long: Long,
    float: Float,
    double: Double,
    // unsigned integer types
    uByte: UByte,
    uShort: UShort,
    uInt: UInt,
    uLong: ULong,
    // string types
    string: String,
    // function types
    function: () -> Unit,
    // external types
    externalInterface: ExternalInterface,
    externalOpenClass: ExternalOpenClass,
    externalObject: ExternalObject,
): Nothing = js("42")

@JsExport
fun correctJsInteropTypesInJsExportFunction(
    // primitive types
    boolean: Boolean,
    char: Char,
    byte: Byte,
    short: Short,
    int: Int,
    long: Long,
    float: Float,
    double: Double,
    // unsigned integer types
    uByte: UByte,
    uShort: UShort,
    uInt: UInt,
    uLong: ULong,
    // string types
    string: String,
    // function types
    function: () -> Unit,
    // external types
    externalInterface: ExternalInterface,
    externalOpenClass: ExternalOpenClass,
    externalObject: ExternalObject,
) {}


typealias AliasedBoolean = Boolean
typealias AliasedChar = Char
typealias AliasedByte = Byte
typealias AliasedShort = Short
typealias AliasedInt = Int
typealias AliasedLong = Long
typealias AliasedFloat = Float
typealias AliasedDouble = Double

typealias AliasedUByte = UByte
typealias AliasedUShort = UShort
typealias AliasedUInt = UInt
typealias AliasedULong = ULong

typealias AliasedString = String

typealias AliasedFunction = () -> Unit

typealias AliasedExternalInterface = ExternalInterface
typealias AliasedExternalOpenClass = ExternalOpenClass
typealias AliasedExternalObject = ExternalObject

external fun aliasedCorrectJsInteropTypes(
    // primitive types
    boolean: AliasedBoolean,
    char: AliasedChar,
    byte: AliasedByte,
    short: AliasedShort,
    int: AliasedInt,
    long: AliasedLong,
    float: AliasedFloat,
    double: AliasedDouble,
    // unsigned integer types
    uByte: AliasedUByte,
    uShort: AliasedUShort,
    uInt: AliasedUInt,
    uLong: AliasedULong,
    // string types
    string: AliasedString,
    // function types
    function: AliasedFunction,
    // external types
    externalInterface: AliasedExternalInterface,
    externalOpenClass: AliasedExternalOpenClass,
    externalObject: AliasedExternalObject,
)
