// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 kotlin/sequences/SequencesKt
// the function arguments of the transformers are evaluated even if the sequence is empty
var calls = 0
fun makeF(): (Int) -> Int {
    calls++
    return { it }
}

fun makeP(): (Int) -> Boolean {
    calls++
    return { true }
}

fun box(): String {
    emptySequence<Int>().map(makeF()).forEach {}
    for (x in emptySequence<Int>().filter(makeP())) {}
    val result = emptySequence<Int>().takeWhile(makeP()).toList()
    return if (calls == 3 && result.isEmpty()) "OK" else "Fail: calls=$calls result=$result"
}
