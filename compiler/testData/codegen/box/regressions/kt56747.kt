// this test was minimized from compiler/testData/codegen/box/coroutines/kt15016.kt
fun mapUnit(transform: (Any) -> Any) = transform(Unit)

fun box(): String {
    mapUnit({ it -> Unit })
    return "OK"
}
