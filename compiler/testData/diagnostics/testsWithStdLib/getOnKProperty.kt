// RUN_PIPELINE_TILL: BACKEND
val Any?.meaning: Int
    get() = 42

fun test() {
    val f = Any?::meaning
    f.get(null)
    f.get("")
}

/* GENERATED_FIR_TAGS: callableReference, functionDeclaration, getter, integerLiteral, localProperty, nullableType,
propertyDeclaration, propertyWithExtensionReceiver, stringLiteral */
