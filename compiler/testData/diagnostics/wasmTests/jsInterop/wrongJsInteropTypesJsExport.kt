// RUN_PIPELINE_TILL: FRONTEND
// OPT_IN: kotlin.js.ExperimentalJsExport
// OPT_IN: kotlin.js.ExperimentalWasmJsInterop
// DIAGNOSTICS: -FINAL_UPPER_BOUND, -UNUSED_PARAMETER
// DIAGNOSTICS: -NON_EXPORTABLE_TYPE

interface UserDefinedInterface
open class UserDefinedOpenClass
object UserDefinedObject


@JsExport
fun wrongJsInteropTypes(
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

@JsExport
fun wrongNullableJsInteropTypes(
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
) {}


// wrong JS interop types as return types

// built-in types
@JsExport
fun anyAsReturnType(): <!WRONG_JS_INTEROP_TYPE!>Any<!> = TODO()
@JsExport
fun numberAsReturnType(): <!WRONG_JS_INTEROP_TYPE!>Number<!> = TODO()
@JsExport
fun charSequenceAsReturnType(): <!WRONG_JS_INTEROP_TYPE!>CharSequence<!> = TODO()
@JsExport
fun specializedArrayAsReturnType(): <!WRONG_JS_INTEROP_TYPE!>IntArray<!> = TODO()
@JsExport
fun pairAsReturnType(): <!WRONG_JS_INTEROP_TYPE!>Pair<Int, Int><!> = TODO()
@JsExport
fun listAsReturnType(): <!WRONG_JS_INTEROP_TYPE!>List<Int><!> = TODO()
@JsExport
fun genericArrayAsReturnType(): <!WRONG_JS_INTEROP_TYPE!>Array<Int><!> = TODO()

// user-defined types
@JsExport
fun userDefinedInterfaceAsReturnType(): <!WRONG_JS_INTEROP_TYPE!>UserDefinedInterface<!> = TODO()
@JsExport
fun userDefinedOpenClassAsReturnType(): <!WRONG_JS_INTEROP_TYPE!>UserDefinedOpenClass<!> = TODO()
@JsExport
fun userDefinedObjectAsReturnType(): <!WRONG_JS_INTEROP_TYPE!>UserDefinedObject<!> = TODO()


@JsExport
fun <
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
) {}


@JsExport
fun wrongJsInteropTypesAsFunctionTypeParameterTypes(
    unitAndNothing: (<!WRONG_JS_INTEROP_TYPE!>Unit<!>, <!WRONG_JS_INTEROP_TYPE!>Nothing<!>) -> Unit,
    builtInTypes: (<!WRONG_JS_INTEROP_TYPE!>Any<!>, <!WRONG_JS_INTEROP_TYPE!>Number<!>, <!WRONG_JS_INTEROP_TYPE!>CharSequence<!>, <!WRONG_JS_INTEROP_TYPE!>IntArray<!>, <!WRONG_JS_INTEROP_TYPE!>Pair<Int, Int><!>, <!WRONG_JS_INTEROP_TYPE!>List<Int><!>, <!WRONG_JS_INTEROP_TYPE!>Array<Int><!>) -> Unit,
    userDefinedTypes: (<!WRONG_JS_INTEROP_TYPE!>UserDefinedInterface<!>, <!WRONG_JS_INTEROP_TYPE!>UserDefinedOpenClass<!>, <!WRONG_JS_INTEROP_TYPE!>UserDefinedObject<!>) -> Unit,
) {}

@JsExport
fun wrongJsInteropTypesAsFunctionTypeReturnTypes(
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

@JsExport
fun aliasedWrongJsInteropTypes(
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
) {}
