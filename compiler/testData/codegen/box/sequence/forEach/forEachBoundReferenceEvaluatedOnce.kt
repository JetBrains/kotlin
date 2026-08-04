// WITH_STDLIB
var calls = 0
class Sink { var sum = 0; fun add(x: Int) { sum += x } }
val sink = Sink()
fun mk(): Sink { calls++; return sink }
fun box(): String {
    sequenceOf(1, 2, 3).forEach(mk()::add)
    return if (sink.sum == 6 && calls == 1) "OK" else "Fail: sum=${sink.sum}, calls=$calls"
}
