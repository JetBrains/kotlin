// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +EagerLambdaAnalysis
// ISSUE: KT-7882

fun <T, R> myWith(receiver: T, f: T.() -> R   ): R    = receiver.f()
fun <T>    myWith(receiver: T, f: T.() -> Unit): Unit = receiver.f()

fun main() {
    myWith("") { subSequence(0, 1) }.length
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, integerLiteral, lambdaLiteral, nullableType, stringLiteral,
typeParameter, typeWithExtension */
