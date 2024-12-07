// OPT_IN: kotlin.js.ExperimentalJsExport

external interface EI
external open class EC
external object EO

external fun complexFunctionTypesWithNonNullTypes(
    f1: (Boolean, Byte, Short, Int, Long, Float, Double, Char, String, EI, EC, EO) -> Unit,
    f2: (Boolean) -> ((Int) -> ((Float) -> ((String) -> EI))),
    f3: ((((Boolean) -> Int) -> Float) -> String) -> EI,
)

external fun complexFunctionTypesWithNullableTypes(
    f1: ((Boolean?, Byte?, Short?, Int?, Long?, Float?, Double?, Char?, String?, EI?, EC?, EO?) -> Unit)?,
    f2: ((Boolean?) -> ((Int?) -> ((Float?) -> ((String?) -> EI)?)?)?)?,
    f3: ((((((((Boolean?) -> Int?)?) -> Float?)?) -> String?)?) -> EI?)?,
)

external fun complexFunctionTypesWithWrongTypes(
    <!WRONG_JS_INTEROP_TYPE!>f1: ((Any?, Byte?, Unit?, Int?, CharSequence?, Float?, IntArray?, Char?, String?, EI?, EC?, EO?) -> Unit)?<!>,
    <!WRONG_JS_INTEROP_TYPE!>f2: ((Boolean?) -> ((Any?) -> ((Float?) -> ((CharSequence?) -> EI)?)?)?)?<!>,
    <!WRONG_JS_INTEROP_TYPE!>f3: ((((((((Boolean?) -> Unit?)?) -> Float?)?) -> IntArray?)?) -> EI?)?<!>,
)

external fun <<!WRONG_JS_INTEROP_TYPE!>T<!>> typeParameterWithUpperBoundsWithDifferentJsInteropCorrectness(arg: T): T where T : EI, T : Any

<!WRONG_JS_INTEROP_TYPE!>fun jsCodeFunctionWithBlockBody(<!WRONG_JS_INTEROP_TYPE!>x: Any<!>): Any<!> { js("return x;") }
<!WRONG_JS_INTEROP_TYPE!>val jsCodeProperty: Any<!> = js("1")
