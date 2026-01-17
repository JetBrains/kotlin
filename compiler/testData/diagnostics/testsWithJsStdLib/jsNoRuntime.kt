// RUN_PIPELINE_TILL: FRONTEND
// OPT_IN: kotlin.js.ExperimentalJsNoRuntime
// DIAGNOSTICS: -UNUSED_VARIABLE

import kotlin.js.JsNoRuntime
import kotlin.js.unsafeCast

@JsNoRuntime
interface I

<!JS_NO_RUNTIME_USELESS_ON_EXTERNAL_INTERFACE!>@JsNoRuntime<!> external interface EI

<!JS_NO_RUNTIME_WRONG_TARGET!>@JsNoRuntime<!> class C
<!JS_NO_RUNTIME_WRONG_TARGET!>@JsNoRuntime<!> object O
<!JS_NO_RUNTIME_WRONG_TARGET!>@JsNoRuntime<!> enum class E { A }

fun isChecks(a: Any) {
    if (<!JS_NO_RUNTIME_FORBIDDEN_IS_CHECK!>a is I<!>) {}
    if (<!JS_NO_RUNTIME_FORBIDDEN_IS_CHECK!>a !is I<!>) {}
}

fun asCasts(a: Any) {
    val x = a <!JS_NO_RUNTIME_FORBIDDEN_AS_CAST!>as<!> I
    val y = a <!USELESS_CAST!><!JS_NO_RUNTIME_FORBIDDEN_AS_CAST!>as?<!> I<!>
}

fun classRef() {
    val k = I::class
}

fun allowedUnsafeCast(a: Any) {
    val i: I = a.unsafeCast<I>()
}

inline fun <reified T> foo(x: T) {}

fun reifiedUsage(ci: I) {
    <!JS_NO_RUNTIME_INTERFACE_AS_REIFIED_TYPE_ARGUMENT!>foo<!>(ci)
    foo<<!JS_NO_RUNTIME_INTERFACE_AS_REIFIED_TYPE_ARGUMENT!>I<!>>(ci)
}
