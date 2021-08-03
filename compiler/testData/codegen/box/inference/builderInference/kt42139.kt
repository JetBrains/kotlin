// DONT_TARGET_EXACT_BACKEND: WASM
// !LANGUAGE: +UnrestrictedBuilderInference
// WITH_RUNTIME
// IGNORE_BACKEND_FIR: JVM_IR

fun <R> select(vararg x: R) = x[0]
fun <K> myEmptyList(): List<K> = emptyList()

fun f1(): Sequence<List<Int>> = sequence {
    yield(myEmptyList())
}

fun f2(): Sequence<List<Int>> = sequence {
    select(<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>yield<!>(<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>myEmptyList<!>()), yield(<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>myEmptyList<!>()))
}

fun f3(): Sequence<List<Int>> = sequence {
    if (true) <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>yield<!>(<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>myEmptyList<!>()) // [NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER] Not enough information to infer type variable T
    else yield(<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>myEmptyList<!>()) // [NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER] Not enough information to infer type variable T
}

fun box(): String {
    f1()
    f2()
    f3()
    return "OK"
}
