// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-30703
// DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE

inline fun go(f: () -> String) = f()

fun builder(c: suspend () -> Unit) {}

suspend fun String.id(): String = this

fun box() {
    val x = ""
    builder {
        go(<!TYPE_MISMATCH!>x::id<!>)
    }
}

/* GENERATED_FIR_TAGS: funWithExtensionReceiver, functionDeclaration, functionalType, inline, lambdaLiteral,
localProperty, propertyDeclaration, stringLiteral, suspend, thisExpression */
