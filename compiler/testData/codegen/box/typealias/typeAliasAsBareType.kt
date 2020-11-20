// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: STDLIB_GENERATED
// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

typealias L<T> = List<T>

fun box(): String {
    val test: Collection<Int> = listOf(1, 2, 3)
    if (test !is L) return "test !is L"
    val test2 = test as L
    if (test.toList() != test2) return "test.toList() != test2"
    return "OK"
}