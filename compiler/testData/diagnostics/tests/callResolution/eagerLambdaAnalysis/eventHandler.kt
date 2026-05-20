// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

fun <E, R> eventHandler(handler: (E) -> R): (E) -> R {
    return handler
}

@JvmName("eventHandler2")
fun <E> eventHandler(handler: (E) -> Unit): (E) -> Unit {
    return handler
}

fun consumeString(x: String) {}

fun main() {
    val x: (String) -> Unit = eventHandler { y -> consumeString(y) }
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, lambdaLiteral, localProperty, nullableType,
propertyDeclaration, typeParameter */
