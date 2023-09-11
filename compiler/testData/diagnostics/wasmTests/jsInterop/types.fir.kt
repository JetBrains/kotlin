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


external fun wrongExternalTypes(
    any: Any,
    nany: Any?,
    unit: Unit,
    nunit: Unit?,
    nothing: Nothing,
    nnothing: Nothing?,
    charSequence: CharSequence,
    list: List<Int>,
    array: Array<Int>,
    intArray: IntArray,
    pair: Pair<Int, Int>,
    number: Number,
)

external fun <T> supportedTypeParamtersUpperBounds(p: T): T where T : EI, T : Any

external fun <
        T1,
        T2 : Number,
        T3: List<Int>,
        > supportedTypeParamtersUpperBounds(
    p1: T1,
    p2: T2
): T3

fun jsCode1(x: Any): Any = js("x")
fun jsCode2(x: Any): Any {
    js("return x;")
}
val jsProp: Any = js("1")

@JsExport
fun exported(x: Any): Any = x

typealias EI_alias = EI
typealias Any_alias = Any
external fun fooAlias(
    ei: EI_alias,
    any: Any_alias,
)
