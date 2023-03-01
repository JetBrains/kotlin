// KT-56747: Wrong return type for `lambda: (Any) -> Any` which returns Unit
// IGNORE_BACKEND_K2: NATIVE

// this test was minimized from compiler/testData/codegen/box/coroutines/kt15016.kt
fun mapUnit(transform: (Any) -> Any) = transform(Unit)

fun box(): String {
    mapUnit({ it -> Unit })
    return "OK"
}
