// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-78252

fun <T : Number> bounded(vararg xs: T) {}
fun <T> unbound(vararg xs: T) {}
fun <T, R : Number> multipleTypeArguments(vararg xs: R) {}
fun <T : Number> inferredParameter(x: T, vararg xs: T) {}

fun createNothing(): Nothing = null!!

fun test() {
    bounded<Nothing>() // throws ClassCastException at runtime
    unbound<Nothing>() // works fine
    multipleTypeArguments<Nothing, Nothing>()
    inferredParameter<_>(createNothing())
    inferredParameter(createNothing())
}

/* GENERATED_FIR_TAGS: functionDeclaration, typeConstraint, typeParameter, vararg */
