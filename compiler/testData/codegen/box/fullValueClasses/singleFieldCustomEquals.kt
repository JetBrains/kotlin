// ISSUE: KT-89978
// LANGUAGE: +FullValueClasses
// Checks that comparisons of full value classes call custom equals of the classes they wrap,
// which the single-field equality optimization must preserve.

value class Modulo(val x: Int) {
    override fun equals(other: Any?): Boolean = other is Modulo && x % 10 == other.x % 10
    override fun hashCode(): Int = x % 10
}

fun box(): String {
    if (Modulo(1) != Modulo(11)) return "Fail: custom equals is not called"
    if (Modulo(1) == Modulo(2)) return "Fail: different values are equal"
    return "OK"
}
