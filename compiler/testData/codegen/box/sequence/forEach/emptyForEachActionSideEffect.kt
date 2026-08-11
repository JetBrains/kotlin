// WITH_STDLIB
var calls = 0
fun makeAction(): (Int) -> Unit { calls++; return {} }
fun box(): String {
    emptySequence<Int>().forEach(makeAction())
    return if (calls == 1) "OK" else "Fail: calls=$calls"
}
