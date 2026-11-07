// ISSUE: KT-91139
// DONT_TARGET_EXACT_BACKEND: JVM
// LANGUAGE: -IrRichCallableReferencesInKlibs
// Compilers up to 2.4.10 which do not write rich callable references to KLIBs store a suspend conversion of a
// function-typed expression as a block with the legacy SUSPEND_CONVERSION origin, renamed to
// FUNCTION_TYPE_EXPRESSION_CONVERSION in 2.4.20. A consumer which doesn't recognize the legacy name lowers the block as
// a plain reference to the synthetic `suspendConversion0` adapter: the converted values become equal to each other and,
// on Wasm and Native, reflectable under the adapter's name. A consumer which recognizes it creates the conversion
// without a reflection target.
// Compilers up to 2.2.21 do not write rich callable references by default. The language feature is disabled to make
// the 2.3.0 first-stage compiler write the block as well; the first-stage runner does not pass the directive to 2.4.0,
// and compilers up to 2.1.0 only warn about the feature, which they do not know.
// The test relies on how KLIB backends compare and reflect such values, hence it is KLIB-only: on the JVM, a suspend
// conversion is a function reference bound to the converted value, so two conversions of the same lambda are equal
// there. compiler/testData/codegen/box/nonLocalReturns/suspendConversion.kt reproduces the issue without relying on
// that.
// WITH_STDLIB
// WITH_COROUTINES

import helpers.*
import kotlin.coroutines.*
import kotlin.reflect.KCallable

fun convert(f: suspend () -> String): suspend () -> String = f

fun toSuspend(f: () -> String): suspend () -> String = convert(f)

val lambda: () -> String = { "lambda" }

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    val a = toSuspend(lambda)
    val b = toSuspend(lambda)

    var result = ""
    builder { result = a() }
    if (result != "lambda") return "Fail: $result"

    // Check both symptoms before failing: which of them shows up depends on the backend.
    val areEqual = a == b
    val name = (a as? KCallable<*>)?.name
    if (areEqual || name != null) return "Fail: equal to another conversion of the same lambda: $areEqual, reflection name: $name"
    return "OK"
}
