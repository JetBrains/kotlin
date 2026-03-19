// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-80047
// WITH_STDLIB

// KT-80047: Type Inference: avoid creating a type variable for the return type of anonymous function with specified return type

fun <T> apply(block: () -> T): T = block()

fun test() {
    // Anonymous function with explicit return type in generic inference context
    // Currently a type variable _R is created even though the return type is explicit
    val x: String = apply(fun(): String = "hello")
    val y: Int = apply(fun(): Int = 42)
    val z: List<String> = apply(fun(): List<String> = listOf("a", "b"))

    // Without expected type - return type should still be inferred correctly
    val a = apply(fun(): String = "world")
    val b = apply(fun(): Int { return 100 })
}

/* GENERATED_FIR_TAGS: anonymousFunction, functionDeclaration, functionalType, integerLiteral, localProperty,
nullableType, propertyDeclaration, stringLiteral, typeParameter */
