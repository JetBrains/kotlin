// RUN_PIPELINE_TILL: FRONTEND
// SKIP_TXT

class Bar {
    val a: Array<String>? = null
}

fun foo(bar: Bar) = bar.a?.asIterable() ?: <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>emptyArray<!>()

fun <T> Array<out T>.asIterable(): Iterable<T> = TODO()

fun testFrontend() {
    val bar = Bar()
    foo(bar)
}

/* GENERATED_FIR_TAGS: classDeclaration, elvisExpression, funWithExtensionReceiver, functionDeclaration, localProperty,
nullableType, outProjection, propertyDeclaration, safeCall, typeParameter */
