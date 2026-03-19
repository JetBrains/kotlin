// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-53307
// WITH_STDLIB

// KT-53307: Type inference problem with Array<T>.copyOf: Array<out Number> causes Unsupported error
fun test() {
    val arr: Array<out Number> = arrayOf(1, 2)
    println(arr.copyOf(4).contentToString())
}

/* GENERATED_FIR_TAGS: capturedType, functionDeclaration, integerLiteral, localProperty, nullableType, outProjection,
propertyDeclaration */
