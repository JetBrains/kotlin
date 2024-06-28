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
    f1: ((<!WRONG_JS_INTEROP_TYPE!>Any?<!>, Byte?, <!WRONG_JS_INTEROP_TYPE!>Unit?<!>, Int?, <!WRONG_JS_INTEROP_TYPE!>CharSequence?<!>, Float?, <!WRONG_JS_INTEROP_TYPE!>IntArray?<!>, Char?, String?, EI?, EC?, EO?) -> Unit)?,
    f2: ((Boolean?) -> ((<!WRONG_JS_INTEROP_TYPE!>Any?<!>) -> ((Float?) -> ((<!WRONG_JS_INTEROP_TYPE!>CharSequence?<!>) -> EI)?)?)?)?,
    f3: ((((((((Boolean?) -> <!WRONG_JS_INTEROP_TYPE!>Unit?<!>)?) -> Float?)?) -> <!WRONG_JS_INTEROP_TYPE!>IntArray?<!>)?) -> EI?)?,
)

external fun <T> typeParameterWithUpperBoundsWithDifferentJsInteropCorrectness(arg: T): T where T : EI, T : <!WRONG_JS_INTEROP_TYPE!>Any<!>

fun jsCodeFunctionWithBlockBody(x: <!WRONG_JS_INTEROP_TYPE!>Any<!>): <!WRONG_JS_INTEROP_TYPE!>Any<!> { js("return x;") }
val jsCodeProperty: <!WRONG_JS_INTEROP_TYPE!>Any<!> = js("1")
