// RUN_PIPELINE_TILL: FRONTEND
// OPT_IN: kotlin.js.ExperimentalJsNoRuntime
// DIAGNOSTICS: -UNUSED_VARIABLE

import kotlin.js.JsNoRuntime
import kotlin.js.unsafeCast

@JsNoRuntime
interface I

@JsNoRuntime
fun interface Runner<T> {
    fun run(): T
}

interface WithRuntime : I

<!JS_NO_RUNTIME_USELESS_ON_EXTERNAL_INTERFACE!>@JsNoRuntime<!> external interface EI

<!JS_NO_RUNTIME_WRONG_TARGET!>@JsNoRuntime<!> class C
<!JS_NO_RUNTIME_WRONG_TARGET!>@JsNoRuntime<!> object O
<!JS_NO_RUNTIME_WRONG_TARGET!>@JsNoRuntime<!> enum class E { A }

fun isChecks(a: Any) {
    if (<!JS_NO_RUNTIME_FORBIDDEN_IS_CHECK!>a is I<!>) {}
    if (<!JS_NO_RUNTIME_FORBIDDEN_IS_CHECK!>a !is I<!>) {}

    if (<!JS_NO_RUNTIME_FORBIDDEN_IS_CHECK!>a is Runner<*><!>) {}
    if (<!JS_NO_RUNTIME_FORBIDDEN_IS_CHECK!>a !is Runner<*><!>) {}

    if (a is WithRuntime) {}
    if (a !is WithRuntime) {}
}

fun asCasts(a: Any) {
    val y = <!JS_NO_RUNTIME_FORBIDDEN_AS_CAST!>a as? I<!>
    val x = <!JS_NO_RUNTIME_FORBIDDEN_AS_CAST!>a as I<!>

    val b = <!JS_NO_RUNTIME_FORBIDDEN_AS_CAST!>a as? Runner<*><!>
    val c = <!JS_NO_RUNTIME_FORBIDDEN_AS_CAST!>a as Runner<*><!>

    val d = a as? WithRuntime
    val e = a as WithRuntime
}

fun classRef() {
    val k = <!JS_NO_RUNTIME_FORBIDDEN_CLASS_REFERENCE!>I::class<!>
    val f = <!JS_NO_RUNTIME_FORBIDDEN_CLASS_REFERENCE!>Runner::class<!>
    val w = WithRuntime::class
}

fun allowedUnsafeCast(a: Any) {
    val i: I = a.unsafeCast<I>()
    val runner: Runner<*> = a.unsafeCast<Runner<*>>()
    val w = a.unsafeCast<WithRuntime>()
}

inline fun <reified T> foo(x: T) {}

fun reifiedUsage(ci: I) {
    <!JS_NO_RUNTIME_INTERFACE_AS_REIFIED_TYPE_ARGUMENT!>foo<!>(ci)
    foo<<!JS_NO_RUNTIME_INTERFACE_AS_REIFIED_TYPE_ARGUMENT!>I<!>>(ci)
}

fun reifiedUsageOfWithRuntime(w: WithRuntime) {
    foo(w)
    foo<WithRuntime>(w)
    foo<<!JS_NO_RUNTIME_INTERFACE_AS_REIFIED_TYPE_ARGUMENT!>I<!>>(w)
}

fun reifiedUsageOfFunInterface(runner: Runner<String>) {
    <!JS_NO_RUNTIME_INTERFACE_AS_REIFIED_TYPE_ARGUMENT!>foo<!>(runner)
    foo<<!JS_NO_RUNTIME_INTERFACE_AS_REIFIED_TYPE_ARGUMENT!>Runner<String><!>>(runner)
}
