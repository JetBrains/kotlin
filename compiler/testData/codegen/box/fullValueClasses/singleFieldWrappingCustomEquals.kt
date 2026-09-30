// LANGUAGE: +FullValueClasses
// WITH_STDLIB

value class Modulo(val x: Int) {
    override fun equals(other: Any?): Boolean = other is Modulo && x % 10 == other.x % 10
    override fun hashCode(): Int = x % 10
}

value class SameLength(val s: String) {
    override fun equals(other: Any?): Boolean = other is SameLength && s.length == other.s.length
    override fun hashCode(): Int = s.length
}

value class Outer(val m: Modulo)
value class OuterOuter(val o: Outer)
value class NullableOuter(val m: Modulo?)
value class StringOuter(val l: SameLength)
data class Container(val o: Outer)

fun box(): String {
    if (Outer(Modulo(1)) != Outer(Modulo(11))) return "Fail 1"
    if (Outer(Modulo(1)) == Outer(Modulo(2))) return "Fail 2"
    if (OuterOuter(Outer(Modulo(1))) != OuterOuter(Outer(Modulo(11)))) return "Fail 3"
    if (NullableOuter(Modulo(1)) != NullableOuter(Modulo(11))) return "Fail 4"
    if (StringOuter(SameLength("a")) != StringOuter(SameLength("b"))) return "Fail 5"
    if (Container(Outer(Modulo(1))) != Container(Outer(Modulo(11)))) return "Fail 6"
    if (setOf(Container(Outer(Modulo(1))), Container(Outer(Modulo(11)))).size != 1) return "Fail 7"
    when (Outer(Modulo(1))) {
        Outer(Modulo(11)) -> {}
        else -> return "Fail 8"
    }
    return "OK"
}
