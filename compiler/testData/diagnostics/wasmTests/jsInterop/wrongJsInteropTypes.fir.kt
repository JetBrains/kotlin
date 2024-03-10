// OPT_IN: kotlin.js.ExperimentalJsExport
// DIAGNOSTICS: -FINAL_UPPER_BOUND, -UNUSED_PARAMETER
// DIAGNOSTICS: -NON_EXPORTABLE_TYPE

interface UserDefinedInterface
open class UserDefinedOpenClass
object UserDefinedObject


external fun wrongJsInteropTypes(
    // Unit and Nothing
    <!WRONG_JS_INTEROP_TYPE!>unit: Unit<!>,
    <!WRONG_JS_INTEROP_TYPE!>nothing: Nothing<!>,
    // built-in types
    <!WRONG_JS_INTEROP_TYPE!>any: Any<!>,
    <!WRONG_JS_INTEROP_TYPE!>number: Number<!>,
    <!WRONG_JS_INTEROP_TYPE!>charSequence: CharSequence<!>,
    <!WRONG_JS_INTEROP_TYPE!>specializedArray: IntArray<!>,
    <!WRONG_JS_INTEROP_TYPE!>pair: Pair<Int, Int><!>,
    <!WRONG_JS_INTEROP_TYPE!>list: List<Int><!>,
    <!WRONG_JS_INTEROP_TYPE!>genericArray: Array<Int><!>,
    // user-defined types
    <!WRONG_JS_INTEROP_TYPE!>userDefinedInterface: UserDefinedInterface<!>,
    <!WRONG_JS_INTEROP_TYPE!>userDefinedOpenClass: UserDefinedOpenClass<!>,
    <!WRONG_JS_INTEROP_TYPE!>userDefinedObject: UserDefinedObject<!>,
)

external fun wrongNullableJsInteropTypes(
    // Unit and Nothing
    <!WRONG_JS_INTEROP_TYPE!>unit: Unit?<!>,
    <!WRONG_JS_INTEROP_TYPE!>nothing: Nothing?<!>,
    // built-in types
    <!WRONG_JS_INTEROP_TYPE!>any: Any?<!>,
    <!WRONG_JS_INTEROP_TYPE!>number: Number?<!>,
    <!WRONG_JS_INTEROP_TYPE!>charSequence: CharSequence?<!>,
    <!WRONG_JS_INTEROP_TYPE!>specializedArray: IntArray?<!>,
    <!WRONG_JS_INTEROP_TYPE!>pair: Pair<Int, Int>?<!>,
    <!WRONG_JS_INTEROP_TYPE!>list: List<Int>?<!>,
    <!WRONG_JS_INTEROP_TYPE!>genericArray: Array<Int>?<!>,
    // user-defined types
    <!WRONG_JS_INTEROP_TYPE!>userDefinedInterface: UserDefinedInterface?<!>,
    <!WRONG_JS_INTEROP_TYPE!>userDefinedOpenClass: UserDefinedOpenClass?<!>,
    <!WRONG_JS_INTEROP_TYPE!>userDefinedObject: UserDefinedObject?<!>,
)


// wrong JS interop types as return types

// built-in types
<!WRONG_JS_INTEROP_TYPE!>external fun anyAsReturnType(): Any<!>
<!WRONG_JS_INTEROP_TYPE!>external fun numberAsReturnType(): Number<!>
<!WRONG_JS_INTEROP_TYPE!>external fun charSequenceAsReturnType(): CharSequence<!>
<!WRONG_JS_INTEROP_TYPE!>external fun specializedArrayAsReturnType(): IntArray<!>
<!WRONG_JS_INTEROP_TYPE!>external fun pairAsReturnType(): Pair<Int, Int><!>
<!WRONG_JS_INTEROP_TYPE!>external fun listAsReturnType(): List<Int><!>
<!WRONG_JS_INTEROP_TYPE!>external fun genericArrayAsReturnType(): Array<Int><!>

// user-defined types
<!WRONG_JS_INTEROP_TYPE!>external fun userDefinedInterfaceAsReturnType(): UserDefinedInterface<!>
<!WRONG_JS_INTEROP_TYPE!>external fun userDefinedOpenClassAsReturnType(): UserDefinedOpenClass<!>
<!WRONG_JS_INTEROP_TYPE!>external fun userDefinedObjectAsReturnType(): UserDefinedObject<!>


// wrong JS interop types as vararg parameter types

// Unit (Nothing is not allowed in vararg parameters)
external fun unitAsVarargParameterType(<!WRONG_JS_INTEROP_TYPE!>vararg args: Unit<!>)

// built-in types
external fun anyAsVarargParameterType(<!WRONG_JS_INTEROP_TYPE!>vararg args: Any<!>)
external fun numberAsVarargParameterType(<!WRONG_JS_INTEROP_TYPE!>vararg args: Number<!>)
external fun charSequenceAsVarargParameterType(<!WRONG_JS_INTEROP_TYPE!>vararg args: CharSequence<!>)
external fun specializedArrayAsVarargParameterType(<!WRONG_JS_INTEROP_TYPE!>vararg args: IntArray<!>)
external fun pairAsVarargParameterType(<!WRONG_JS_INTEROP_TYPE!>vararg args: Pair<Int, Int><!>)
external fun listAsVarargParameterType(<!WRONG_JS_INTEROP_TYPE!>vararg args: List<Int><!>)
external fun genericArrayAsVarargParameterType(<!WRONG_JS_INTEROP_TYPE!>vararg args: Array<Int><!>)

// user-defined types
external fun userDefinedInterfaceAsVarargParameterType(<!WRONG_JS_INTEROP_TYPE!>vararg args: UserDefinedInterface<!>)
external fun userDefinedOpenClassAsVarargParameterType(<!WRONG_JS_INTEROP_TYPE!>vararg args: UserDefinedOpenClass<!>)
external fun userDefinedObjectAsVarargParameterType(<!WRONG_JS_INTEROP_TYPE!>vararg args: UserDefinedObject<!>)


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
        TUnit: <!WRONG_JS_INTEROP_TYPE, WRONG_JS_INTEROP_TYPE!>Unit<!>,
        // built-in types
        TAny: <!WRONG_JS_INTEROP_TYPE, WRONG_JS_INTEROP_TYPE!>Any<!>,
        TNumber: <!WRONG_JS_INTEROP_TYPE, WRONG_JS_INTEROP_TYPE!>Number<!>,
        TCharSequence: <!WRONG_JS_INTEROP_TYPE, WRONG_JS_INTEROP_TYPE!>CharSequence<!>,
        TSpecializedArray: <!WRONG_JS_INTEROP_TYPE, WRONG_JS_INTEROP_TYPE!>IntArray<!>,
        TPair: <!WRONG_JS_INTEROP_TYPE, WRONG_JS_INTEROP_TYPE!>Pair<Int, Int><!>,
        TList: <!WRONG_JS_INTEROP_TYPE, WRONG_JS_INTEROP_TYPE!>List<Int><!>,
        TGenericArray: <!WRONG_JS_INTEROP_TYPE, WRONG_JS_INTEROP_TYPE!>Array<Int><!>,
        // user-defined types
        TUserDefinedInterface: <!WRONG_JS_INTEROP_TYPE, WRONG_JS_INTEROP_TYPE!>UserDefinedInterface<!>,
        TUserDefinedOpenClass: <!WRONG_JS_INTEROP_TYPE, WRONG_JS_INTEROP_TYPE!>UserDefinedOpenClass<!>,
        TUserDefinedObject: <!WRONG_JS_INTEROP_TYPE, WRONG_JS_INTEROP_TYPE!>UserDefinedObject<!>,
        // type parameter with implicit upper bound
        <!WRONG_JS_INTEROP_TYPE, WRONG_JS_INTEROP_TYPE!>TTypeParameterWithImplicitUpperBound<!>
>


// wrong JS interop types as property types

// Unit and Nothing
<!WRONG_JS_INTEROP_TYPE!>external val unitProperty: Unit<!>
<!WRONG_JS_INTEROP_TYPE!>external val nothingProperty: Nothing<!>

// built-in types
<!WRONG_JS_INTEROP_TYPE, WRONG_JS_INTEROP_TYPE!>external val anyProperty: Any<!>
<!WRONG_JS_INTEROP_TYPE, WRONG_JS_INTEROP_TYPE!>external val numberProperty: Number<!>
<!WRONG_JS_INTEROP_TYPE, WRONG_JS_INTEROP_TYPE!>external val charSequenceProperty: CharSequence<!>
<!WRONG_JS_INTEROP_TYPE, WRONG_JS_INTEROP_TYPE!>external val specializedArrayProperty: IntArray<!>
<!WRONG_JS_INTEROP_TYPE, WRONG_JS_INTEROP_TYPE!>external val pairProperty: Pair<Int, Int><!>
<!WRONG_JS_INTEROP_TYPE, WRONG_JS_INTEROP_TYPE!>external val listProperty: List<Int><!>
<!WRONG_JS_INTEROP_TYPE, WRONG_JS_INTEROP_TYPE!>external val genericArrayProperty: Array<Int><!>

// user-defined types
<!WRONG_JS_INTEROP_TYPE, WRONG_JS_INTEROP_TYPE!>external val userDefinedInterfaceProperty: UserDefinedInterface<!>
<!WRONG_JS_INTEROP_TYPE, WRONG_JS_INTEROP_TYPE!>external val userDefinedOpenClassProperty: UserDefinedOpenClass<!>
<!WRONG_JS_INTEROP_TYPE, WRONG_JS_INTEROP_TYPE!>external val userDefinedObjectProperty: UserDefinedObject<!>


external fun wrongJsInteropTypesAsFunctionTypeParameterTypes(
    <!WRONG_JS_INTEROP_TYPE!>unitAndNothing: (Unit, Nothing) -> Unit<!>,
    <!WRONG_JS_INTEROP_TYPE!>builtInTypes: (Any, Number, CharSequence, IntArray, Pair<Int, Int>, List<Int>, Array<Int>) -> Unit<!>,
    <!WRONG_JS_INTEROP_TYPE!>userDefinedTypes: (UserDefinedInterface, UserDefinedOpenClass, UserDefinedObject) -> Unit<!>,
)

external fun wrongJsInteropTypesAsFunctionTypeReturnTypes(
    // built-in types
    <!WRONG_JS_INTEROP_TYPE!>any: () -> Any<!>,
    <!WRONG_JS_INTEROP_TYPE!>number: () -> Number<!>,
    <!WRONG_JS_INTEROP_TYPE!>charSequence: () -> CharSequence<!>,
    <!WRONG_JS_INTEROP_TYPE!>specializedArray: () -> IntArray<!>,
    <!WRONG_JS_INTEROP_TYPE!>pair: () -> Pair<Int, Int><!>,
    <!WRONG_JS_INTEROP_TYPE!>list: () -> List<Int><!>,
    <!WRONG_JS_INTEROP_TYPE!>genericArray: () -> Array<Int><!>,
    // user-defined types
    <!WRONG_JS_INTEROP_TYPE!>userDefinedInterface: () -> UserDefinedInterface<!>,
    <!WRONG_JS_INTEROP_TYPE!>userDefinedOpenClass: () -> UserDefinedOpenClass<!>,
    <!WRONG_JS_INTEROP_TYPE!>userDefinedObject: () -> UserDefinedObject<!>,
)

fun wrongJsInteropTypesInJsCodeFunction(
    // Unit and Nothing
    <!WRONG_JS_INTEROP_TYPE!>unit: Unit<!>,
    <!WRONG_JS_INTEROP_TYPE!>nothing: Nothing<!>,
    // built-in types
    <!WRONG_JS_INTEROP_TYPE!>any: Any<!>,
    <!WRONG_JS_INTEROP_TYPE!>number: Number<!>,
    <!WRONG_JS_INTEROP_TYPE!>charSequence: CharSequence<!>,
    <!WRONG_JS_INTEROP_TYPE!>specializedArray: IntArray<!>,
    <!WRONG_JS_INTEROP_TYPE!>pair: Pair<Int, Int><!>,
    <!WRONG_JS_INTEROP_TYPE!>list: List<Int><!>,
    <!WRONG_JS_INTEROP_TYPE!>genericArray: Array<Int><!>,
    // user-defined types
    <!WRONG_JS_INTEROP_TYPE!>userDefinedInterface: UserDefinedInterface<!>,
    <!WRONG_JS_INTEROP_TYPE!>userDefinedOpenClass: UserDefinedOpenClass<!>,
    <!WRONG_JS_INTEROP_TYPE!>userDefinedObject: UserDefinedObject<!>,
): Nothing = js("42")

@JsExport
fun wrongJsInteropTypesInJsExportFunction(
    // Unit and Nothing
    <!WRONG_JS_INTEROP_TYPE!>unit: Unit<!>,
    <!WRONG_JS_INTEROP_TYPE!>nothing: Nothing<!>,
    // built-in types
    <!WRONG_JS_INTEROP_TYPE!>any: Any<!>,
    <!WRONG_JS_INTEROP_TYPE!>number: Number<!>,
    <!WRONG_JS_INTEROP_TYPE!>charSequence: CharSequence<!>,
    <!WRONG_JS_INTEROP_TYPE!>specializedArray: IntArray<!>,
    <!WRONG_JS_INTEROP_TYPE!>pair: Pair<Int, Int><!>,
    <!WRONG_JS_INTEROP_TYPE!>list: List<Int><!>,
    <!WRONG_JS_INTEROP_TYPE!>genericArray: Array<Int><!>,
    // user-defined types
    <!WRONG_JS_INTEROP_TYPE!>userDefinedInterface: UserDefinedInterface<!>,
    <!WRONG_JS_INTEROP_TYPE!>userDefinedOpenClass: UserDefinedOpenClass<!>,
    <!WRONG_JS_INTEROP_TYPE!>userDefinedObject: UserDefinedObject<!>,
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
    <!WRONG_JS_INTEROP_TYPE!>unit: AliasedUnit<!>,
    <!WRONG_JS_INTEROP_TYPE!>nothing: AliasedNothing<!>,
    // built-in types
    <!WRONG_JS_INTEROP_TYPE!>any: AliasedAny<!>,
    <!WRONG_JS_INTEROP_TYPE!>number: AliasedNumber<!>,
    <!WRONG_JS_INTEROP_TYPE!>charSequence: AliasedCharSequence<!>,
    <!WRONG_JS_INTEROP_TYPE!>specializedArray: AliasedSpecializedArray<!>,
    <!WRONG_JS_INTEROP_TYPE!>pair: AliasedPair<!>,
    <!WRONG_JS_INTEROP_TYPE!>list: AliasedList<!>,
    <!WRONG_JS_INTEROP_TYPE!>genericArray: AliasedGenericArray<!>,
    // user-defined types
    <!WRONG_JS_INTEROP_TYPE!>userDefinedInterface: AliasedUserDefinedInterface<!>,
    <!WRONG_JS_INTEROP_TYPE!>userDefinedOpenClass: AliasedUserDefinedOpenClass<!>,
    <!WRONG_JS_INTEROP_TYPE!>userDefinedObject: AliasedUserDefinedObject<!>,
)
