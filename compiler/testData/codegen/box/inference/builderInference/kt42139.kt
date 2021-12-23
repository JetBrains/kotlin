// !LANGUAGE: +UnrestrictedBuilderInference
// WITH_STDLIB

fun <R> select(vararg x: R) = x[0]
fun <K> myEmptyList(): List<K> = emptyList()

fun f1(): Sequence<List<Int>> = sequence {
    yield(myEmptyList())
}

fun f2(): Sequence<List<Int>> = sequence {
    select(yield(myEmptyList()), yield(myEmptyList()))
}

fun f3(): Sequence<List<Int>> = sequence {
    if (true) yield(myEmptyList()) // [NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER] Not enough information to infer type variable T
    else yield(myEmptyList()) // [NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER] Not enough information to infer type variable T
}

fun box(): String {
    f1()
    f2()
    f3()
    return "OK"
}
