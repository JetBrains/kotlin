// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: +UNUSED_LAMBDA_EXPRESSION

fun main() {
    "".run {
        <!UNUSED_LAMBDA_EXPRESSION!>{}<!>
    }
}


fun <T> T.run(f: (T) -> Unit): Unit = f(this)

/* GENERATED_FIR_TAGS: funWithExtensionReceiver, functionDeclaration, functionalType, lambdaLiteral, nullableType,
stringLiteral, thisExpression, typeParameter */
