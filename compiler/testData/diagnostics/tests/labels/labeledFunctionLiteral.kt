// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -UNUSED_VARIABLE

val funLit = lambda@ fun String.() {
    val d1 = this@lambda
}

fun test() {
    val funLit = lambda@ fun String.(): String {
        return this@lambda
    }
}

fun lambda() {
    val funLit = lambda@ fun String.(): String {
        return this@lambda
    }
}

/* GENERATED_FIR_TAGS: anonymousFunction, functionDeclaration, localProperty, propertyDeclaration, thisExpression */
