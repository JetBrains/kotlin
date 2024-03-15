// OPT_IN: kotlin.js.ExperimentalJsExport
// DIAGNOSTICS: -FINAL_UPPER_BOUND, -UNUSED_PARAMETER
// DIAGNOSTICS: -NON_EXPORTABLE_TYPE

interface UserDefinedInterface
open class UserDefinedOpenClass
object UserDefinedObject


external fun wrongJsInteropTypes(
    // Unit and Nothing
    unit: <!WRONG_JS_INTEROP_TYPE!>Unit<!>,
    nothing: <!WRONG_JS_INTEROP_TYPE!>Nothing<!>,
    // built-in types
    any: <!WRONG_JS_INTEROP_TYPE!>Any<!>,
    number: <!WRONG_JS_INTEROP_TYPE!>Number<!>,
    charSequence: <!WRONG_JS_INTEROP_TYPE!>CharSequence<!>,
    specializedArray: <!WRONG_JS_INTEROP_TYPE!>IntArray<!>,
    pair: <!WRONG_JS_INTEROP_TYPE!>Pair<Int, Int><!>,
    list: <!WRONG_JS_INTEROP_TYPE!>List<Int><!>,
    genericArray: <!WRONG_JS_INTEROP_TYPE!>Array<Int><!>,
    // user-defined types
    userDefinedInterface: <!WRONG_JS_INTEROP_TYPE!>UserDefinedInterface<!>,
    userDefinedOpenClass: <!WRONG_JS_INTEROP_TYPE!>UserDefinedOpenClass<!>,
    userDefinedObject: <!WRONG_JS_INTEROP_TYPE!>UserDefinedObject<!>,
)

external fun wrongNullableJsInteropTypes(
    // Unit and Nothing
    unit: <!WRONG_JS_INTEROP_TYPE!>Unit?<!>,
    nothing: <!WRONG_JS_INTEROP_TYPE!>Nothing?<!>,
    // built-in types
    any: <!WRONG_JS_INTEROP_TYPE!>Any?<!>,
    number: <!WRONG_JS_INTEROP_TYPE!>Number?<!>,
    charSequence: <!WRONG_JS_INTEROP_TYPE!>CharSequence?<!>,
    specializedArray: <!WRONG_JS_INTEROP_TYPE!>IntArray?<!>,
    pair: <!WRONG_JS_INTEROP_TYPE!>Pair<Int, Int>?<!>,
    list: <!WRONG_JS_INTEROP_TYPE!>List<Int>?<!>,
    genericArray: <!WRONG_JS_INTEROP_TYPE!>Array<Int>?<!>,
    // user-defined types
    userDefinedInterface: <!WRONG_JS_INTEROP_TYPE!>UserDefinedInterface?<!>,
    userDefinedOpenClass: <!WRONG_JS_INTEROP_TYPE!>UserDefinedOpenClass?<!>,
    userDefinedObject: <!WRONG_JS_INTEROP_TYPE!>UserDefinedObject?<!>,
)


// wrong JS interop types as return types

// built-in types
external fun anyAsReturnType(): <!WRONG_JS_INTEROP_TYPE!>Any<!>
external fun numberAsReturnType(): <!WRONG_JS_INTEROP_TYPE!>Number<!>
external fun charSequenceAsReturnType(): <!WRONG_JS_INTEROP_TYPE!>CharSequence<!>
external fun specializedArrayAsReturnType(): <!WRONG_JS_INTEROP_TYPE!>IntArray<!>
external fun pairAsReturnType(): <!WRONG_JS_INTEROP_TYPE!>Pair<Int, Int><!>
external fun listAsReturnType(): <!WRONG_JS_INTEROP_TYPE!>List<Int><!>
external fun genericArrayAsReturnType(): <!WRONG_JS_INTEROP_TYPE!>Array<Int><!>

// user-defined types
external fun userDefinedInterfaceAsReturnType(): <!WRONG_JS_INTEROP_TYPE!>UserDefinedInterface<!>
external fun userDefinedOpenClassAsReturnType(): <!WRONG_JS_INTEROP_TYPE!>UserDefinedOpenClass<!>
external fun userDefinedObjectAsReturnType(): <!WRONG_JS_INTEROP_TYPE!>UserDefinedObject<!>


// wrong JS interop types as vararg parameter types

// Unit (Nothing is not allowed in vararg parameters)
external fun unitAsVarargParameterType(vararg args: <!WRONG_JS_INTEROP_TYPE!>Unit<!>)

// built-in types
external fun anyAsVarargParameterType(vararg args: <!WRONG_JS_INTEROP_TYPE!>Any<!>)
external fun numberAsVarargParameterType(vararg args: <!WRONG_JS_INTEROP_TYPE!>Number<!>)
external fun charSequenceAsVarargParameterType(vararg args: <!WRONG_JS_INTEROP_TYPE!>CharSequence<!>)
external fun specializedArrayAsVarargParameterType(vararg args: <!WRONG_JS_INTEROP_TYPE!>IntArray<!>)
external fun pairAsVarargParameterType(vararg args: <!WRONG_JS_INTEROP_TYPE!>Pair<Int, Int><!>)
external fun listAsVarargParameterType(vararg args: <!WRONG_JS_INTEROP_TYPE!>List<Int><!>)
external fun genericArrayAsVarargParameterType(vararg args: <!WRONG_JS_INTEROP_TYPE!>Array<Int><!>)

// user-defined types
external fun userDefinedInterfaceAsVarargParameterType(vararg args: <!WRONG_JS_INTEROP_TYPE!>UserDefinedInterface<!>)
external fun userDefinedOpenClassAsVarargParameterType(vararg args: <!WRONG_JS_INTEROP_TYPE!>UserDefinedOpenClass<!>)
external fun userDefinedObjectAsVarargParameterType(vararg args: <!WRONG_JS_INTEROP_TYPE!>UserDefinedObject<!>)


external fun <
        // Unit (Nothing as an upper bound results in an empty intersection type)
        TUnit: <!WRONG_JS_INTEROP_TYPE!>Unit<!>,
        // built-in types
        TAny: <!WRONG_JS_INTEROP_TYPE!>Any<!>,
        TNumber: <!WRONG_JS_INTEROP_TYPE!>Number<!>,
        TCharSequence: <!WRONG_JS_INTEROP_TYPE!>CharSequence<!>,
        TSpecializedArray: <!WRONG_JS_INTEROP_TYPE!>IntArray<!>,
        TPair: <!WRONG_JS_INTEROP_TYPE!>Pair<Int, Int><!>,
        TList: <!WRONG_JS_INTEROP_TYPE!>List<Int><!>,
        TGenericArray: <!WRONG_JS_INTEROP_TYPE!>Array<Int><!>,
        // user-defined types
        TUserDefinedInterface: <!WRONG_JS_INTEROP_TYPE!>UserDefinedInterface<!>,
        TUserDefinedOpenClass: <!WRONG_JS_INTEROP_TYPE!>UserDefinedOpenClass<!>,
        TUserDefinedObject: <!WRONG_JS_INTEROP_TYPE!>UserDefinedObject<!>,
        // type parameter with implicit upper bound
        <!WRONG_JS_INTEROP_TYPE!>TTypeParameterWithImplicitUpperBound<!>
> wrongJsInteropTypesAsFunctionTypeParameterUpperBounds(
    unit: TUnit,
    any: TAny,
    number: TNumber,
    charSequence: TCharSequence,
    specializedArray: TSpecializedArray,
    pair: TPair,
    list: TList,
    genericArray: TGenericArray,
    userDefinedInterface: TUserDefinedInterface,
    userDefinedOpenClass: TUserDefinedOpenClass,
    userDefinedObject: TUserDefinedObject,
    typeParameterWithImplicitUpperBound: TTypeParameterWithImplicitUpperBound
)

external class WrongJsInteropTypesAsClassTypeParameterUpperBounds<
        // Unit (Nothing as an upper bound results in an empty intersection type)
        TUnit: <!WRONG_JS_INTEROP_TYPE!>Unit<!>,
        // built-in types
        TAny: <!WRONG_JS_INTEROP_TYPE!>Any<!>,
        TNumber: <!WRONG_JS_INTEROP_TYPE!>Number<!>,
        TCharSequence: <!WRONG_JS_INTEROP_TYPE!>CharSequence<!>,
        TSpecializedArray: <!WRONG_JS_INTEROP_TYPE!>IntArray<!>,
        TPair: <!WRONG_JS_INTEROP_TYPE!>Pair<Int, Int><!>,
        TList: <!WRONG_JS_INTEROP_TYPE!>List<Int><!>,
        TGenericArray: <!WRONG_JS_INTEROP_TYPE!>Array<Int><!>,
        // user-defined types
        TUserDefinedInterface: <!WRONG_JS_INTEROP_TYPE!>UserDefinedInterface<!>,
        TUserDefinedOpenClass: <!WRONG_JS_INTEROP_TYPE!>UserDefinedOpenClass<!>,
        TUserDefinedObject: <!WRONG_JS_INTEROP_TYPE!>UserDefinedObject<!>,
        // type parameter with implicit upper bound
        <!WRONG_JS_INTEROP_TYPE!>TTypeParameterWithImplicitUpperBound<!>
>


// wrong JS interop types as property types

// Unit and Nothing
external val unitProperty: <!WRONG_JS_INTEROP_TYPE!>Unit<!>
external val nothingProperty: <!WRONG_JS_INTEROP_TYPE!>Nothing<!>

// built-in types
external val anyProperty: <!WRONG_JS_INTEROP_TYPE!>Any<!>
external val numberProperty: <!WRONG_JS_INTEROP_TYPE!>Number<!>
external val charSequenceProperty: <!WRONG_JS_INTEROP_TYPE!>CharSequence<!>
external val specializedArrayProperty: <!WRONG_JS_INTEROP_TYPE!>IntArray<!>
external val pairProperty: <!WRONG_JS_INTEROP_TYPE!>Pair<Int, Int><!>
external val listProperty: <!WRONG_JS_INTEROP_TYPE!>List<Int><!>
external val genericArrayProperty: <!WRONG_JS_INTEROP_TYPE!>Array<Int><!>

// user-defined types
external val userDefinedInterfaceProperty: <!WRONG_JS_INTEROP_TYPE!>UserDefinedInterface<!>
external val userDefinedOpenClassProperty: <!WRONG_JS_INTEROP_TYPE!>UserDefinedOpenClass<!>
external val userDefinedObjectProperty: <!WRONG_JS_INTEROP_TYPE!>UserDefinedObject<!>


external fun wrongJsInteropTypesAsFunctionTypeParameterTypes(
    unitAndNothing: (<!WRONG_JS_INTEROP_TYPE!>Unit<!>, <!WRONG_JS_INTEROP_TYPE!>Nothing<!>) -> Unit,
    builtInTypes: (<!WRONG_JS_INTEROP_TYPE!>Any<!>, <!WRONG_JS_INTEROP_TYPE!>Number<!>, <!WRONG_JS_INTEROP_TYPE!>CharSequence<!>, <!WRONG_JS_INTEROP_TYPE!>IntArray<!>, <!WRONG_JS_INTEROP_TYPE!>Pair<Int, Int><!>, <!WRONG_JS_INTEROP_TYPE!>List<Int><!>, <!WRONG_JS_INTEROP_TYPE!>Array<Int><!>) -> Unit,
    userDefinedTypes: (<!WRONG_JS_INTEROP_TYPE!>UserDefinedInterface<!>, <!WRONG_JS_INTEROP_TYPE!>UserDefinedOpenClass<!>, <!WRONG_JS_INTEROP_TYPE!>UserDefinedObject<!>) -> Unit,
)

external fun wrongJsInteropTypesAsFunctionTypeReturnTypes(
    // built-in types
    any: () -> <!WRONG_JS_INTEROP_TYPE!>Any<!>,
    number: () -> <!WRONG_JS_INTEROP_TYPE!>Number<!>,
    charSequence: () -> <!WRONG_JS_INTEROP_TYPE!>CharSequence<!>,
    specializedArray: () -> <!WRONG_JS_INTEROP_TYPE!>IntArray<!>,
    pair: () -> <!WRONG_JS_INTEROP_TYPE!>Pair<Int, Int><!>,
    list: () -> <!WRONG_JS_INTEROP_TYPE!>List<Int><!>,
    genericArray: () -> <!WRONG_JS_INTEROP_TYPE!>Array<Int><!>,
    // user-defined types
    userDefinedInterface: () -> <!WRONG_JS_INTEROP_TYPE!>UserDefinedInterface<!>,
    userDefinedOpenClass: () -> <!WRONG_JS_INTEROP_TYPE!>UserDefinedOpenClass<!>,
    userDefinedObject: () -> <!WRONG_JS_INTEROP_TYPE!>UserDefinedObject<!>,
)

fun wrongJsInteropTypesInJsCodeFunction(
    // Unit and Nothing
    unit: <!WRONG_JS_INTEROP_TYPE!>Unit<!>,
    nothing: <!WRONG_JS_INTEROP_TYPE!>Nothing<!>,
    // built-in types
    any: <!WRONG_JS_INTEROP_TYPE!>Any<!>,
    number: <!WRONG_JS_INTEROP_TYPE!>Number<!>,
    charSequence: <!WRONG_JS_INTEROP_TYPE!>CharSequence<!>,
    specializedArray: <!WRONG_JS_INTEROP_TYPE!>IntArray<!>,
    pair: <!WRONG_JS_INTEROP_TYPE!>Pair<Int, Int><!>,
    list: <!WRONG_JS_INTEROP_TYPE!>List<Int><!>,
    genericArray: <!WRONG_JS_INTEROP_TYPE!>Array<Int><!>,
    // user-defined types
    userDefinedInterface: <!WRONG_JS_INTEROP_TYPE!>UserDefinedInterface<!>,
    userDefinedOpenClass: <!WRONG_JS_INTEROP_TYPE!>UserDefinedOpenClass<!>,
    userDefinedObject: <!WRONG_JS_INTEROP_TYPE!>UserDefinedObject<!>,
): Nothing = js("42")

@JsExport
fun wrongJsInteropTypesInJsExportFunction(
    // Unit and Nothing
    unit: <!WRONG_JS_INTEROP_TYPE!>Unit<!>,
    nothing: <!WRONG_JS_INTEROP_TYPE!>Nothing<!>,
    // built-in types
    any: <!WRONG_JS_INTEROP_TYPE!>Any<!>,
    number: <!WRONG_JS_INTEROP_TYPE!>Number<!>,
    charSequence: <!WRONG_JS_INTEROP_TYPE!>CharSequence<!>,
    specializedArray: <!WRONG_JS_INTEROP_TYPE!>IntArray<!>,
    pair: <!WRONG_JS_INTEROP_TYPE!>Pair<Int, Int><!>,
    list: <!WRONG_JS_INTEROP_TYPE!>List<Int><!>,
    genericArray: <!WRONG_JS_INTEROP_TYPE!>Array<Int><!>,
    // user-defined types
    userDefinedInterface: <!WRONG_JS_INTEROP_TYPE!>UserDefinedInterface<!>,
    userDefinedOpenClass: <!WRONG_JS_INTEROP_TYPE!>UserDefinedOpenClass<!>,
    userDefinedObject: <!WRONG_JS_INTEROP_TYPE!>UserDefinedObject<!>,
) {}


typealias AliasedUnit = Unit
typealias AliasedNothing = Nothing

typealias AliasedAny = Any
typealias AliasedNumber = Number
typealias AliasedCharSequence = CharSequence
typealias AliasedSpecializedArray = IntArray
typealias AliasedPair = Pair<Int, Int>
typealias AliasedList = List<Int>
typealias AliasedGenericArray = Array<Int>

typealias AliasedUserDefinedInterface = UserDefinedInterface
typealias AliasedUserDefinedOpenClass = UserDefinedOpenClass
typealias AliasedUserDefinedObject = UserDefinedObject

external fun aliasedWrongJsInteropTypes(
    // Unit and Nothing
    unit: <!WRONG_JS_INTEROP_TYPE!>AliasedUnit<!>,
    nothing: <!WRONG_JS_INTEROP_TYPE!>AliasedNothing<!>,
    // built-in types
    any: <!WRONG_JS_INTEROP_TYPE!>AliasedAny<!>,
    number: <!WRONG_JS_INTEROP_TYPE!>AliasedNumber<!>,
    charSequence: <!WRONG_JS_INTEROP_TYPE!>AliasedCharSequence<!>,
    specializedArray: <!WRONG_JS_INTEROP_TYPE!>AliasedSpecializedArray<!>,
    pair: <!WRONG_JS_INTEROP_TYPE!>AliasedPair<!>,
    list: <!WRONG_JS_INTEROP_TYPE!>AliasedList<!>,
    genericArray: <!WRONG_JS_INTEROP_TYPE!>AliasedGenericArray<!>,
    // user-defined types
    userDefinedInterface: <!WRONG_JS_INTEROP_TYPE!>AliasedUserDefinedInterface<!>,
    userDefinedOpenClass: <!WRONG_JS_INTEROP_TYPE!>AliasedUserDefinedOpenClass<!>,
    userDefinedObject: <!WRONG_JS_INTEROP_TYPE!>AliasedUserDefinedObject<!>,
)
