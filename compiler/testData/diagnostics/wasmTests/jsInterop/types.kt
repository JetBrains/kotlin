// !OPT_IN: kotlin.js.ExperimentalJsExport

external interface EI
external open class EC
external object EO

external fun supportedTypes(
    boolean: Boolean,

    byte: Byte,
    short: Short,
    int: Int,
    long: Long,

    float: Float,
    double: Double,

    char: Char,
    string: String,

    ei: EI,
    ec: EC,
    eo: EO,

    f1: (Boolean, Byte, Short, Int, Long, Float, Double, Char, String, EI, EC, EO) -> Unit,

    f2: (Boolean) -> ((Int) -> ((Float) -> ((String) -> EI))),

    f3: ((((Boolean) -> Int) -> Float) -> String) -> EI,
): Unit

external fun supportedNullableTypes(
    boolean: Boolean?,

    byte: Byte?,
    short: Short?,
    int: Int?,
    long: Long?,

    float: Float?,
    double: Double?,

    char: Char?,
    string: String?,

    ei: EI?,
    ec: EC?,
    eo: EO?,

    f1: ((Boolean?, Byte?, Short?, Int?, Long?, Float?, Double?, Char?, String?, EI?, EC?, EO?) -> Unit)?,

    f2: ((Boolean?) -> ((Int?) -> ((Float?) -> ((String?) -> EI)?)?)?)?,

    f3: ((((((((Boolean?) -> Int?)?) -> Float?)?) -> String?)?) -> EI?)?,
): Unit

external fun supportedReturnTypeUnit(): Unit
external fun supportedReturnTypeNothing(): Nothing
external fun supportedReturnTypeBoolean(): Boolean
external fun supportedReturnTypeNullableInt(): Int?
external fun supportedReturnTypeEI(): EI
external fun supportedReturnTypeNullableEC(): EC?

external fun <
        T1 : EI,
        T2 : EC?,
        T3 : T1?
        > supportedTypeParamtersUpperBounds(p1: T1, p2: T2): T3

external fun supportedVararg(vararg p: Int)

external fun wrongExternalTypes(
    <!WRONG_JS_INTEROP_TYPE!>any: Any<!>,
    <!WRONG_JS_INTEROP_TYPE!>nany: Any?<!>,
    <!WRONG_JS_INTEROP_TYPE!>unit: Unit<!>,
    <!WRONG_JS_INTEROP_TYPE!>nunit: Unit?<!>,
    <!WRONG_JS_INTEROP_TYPE!>nothing: Nothing<!>,
    <!WRONG_JS_INTEROP_TYPE!>nnothing: Nothing?<!>,
    <!WRONG_JS_INTEROP_TYPE!>charSequence: CharSequence<!>,
    <!WRONG_JS_INTEROP_TYPE!>list: List<Int><!>,
    <!WRONG_JS_INTEROP_TYPE!>array: Array<Int><!>,
    <!WRONG_JS_INTEROP_TYPE!>intArray: IntArray<!>,
    <!WRONG_JS_INTEROP_TYPE!>pair: Pair<Int, Int><!>,
    <!WRONG_JS_INTEROP_TYPE!>number: Number<!>,
)

external fun wrongVararg(<!WRONG_JS_INTEROP_TYPE!>vararg p: Any<!>)

external fun <<!WRONG_JS_INTEROP_TYPE!>T<!>> supportedTypeParamtersUpperBounds(p: T): T where T : EI, T : Any

external fun <
        <!WRONG_JS_INTEROP_TYPE!>T1<!>,
        <!WRONG_JS_INTEROP_TYPE!>T2 : Number<!>,
        <!WRONG_JS_INTEROP_TYPE!>T3: List<Int><!>,
        > supportedTypeParamtersUpperBounds(
    p1: T1,
    p2: T2
): T3

<!WRONG_JS_INTEROP_TYPE!>fun jsCode1(<!WRONG_JS_INTEROP_TYPE!>x: Any<!>): Any<!> = js("x")
<!WRONG_JS_INTEROP_TYPE!>fun jsCode2(<!WRONG_JS_INTEROP_TYPE!>x: Any<!>): Any<!> {
    js("return x;")
}
<!WRONG_JS_INTEROP_TYPE!>val jsProp: Any<!> = js("1")

<!WRONG_JS_INTEROP_TYPE!>@JsExport
fun exported(<!WRONG_JS_INTEROP_TYPE!>x: Any<!>): Any<!> = x

typealias EI_alias = EI
typealias Any_alias = Any
typealias Function_alias = (Int) -> Int
external fun fooAlias(
    ei: EI_alias,
    <!WRONG_JS_INTEROP_TYPE!>any: Any_alias<!>,
    function: Function_alias,
)
