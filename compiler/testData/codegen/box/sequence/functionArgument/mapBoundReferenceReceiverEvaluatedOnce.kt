// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 kotlin/sequences/SequencesKt

var calls = 0
class A { fun f(x: Int) = x * 2 }
fun mk(): A { calls++; return A() }
fun box(): String {
    val r = sequenceOf(1, 2, 3).map(mk()::f).toList()
    return if (r == listOf(2, 4, 6) && calls == 1) "OK" else "Fail: $r, calls=$calls"
}
