// Strictly speaking, asymmetric equals violates contract for 'Object#equals'.
// However, we don't rely on this contract so far.
class FakeInt(val value: Int) {
    override fun equals(other: Any?): Boolean =
            other is Int && other == value
}

fun box(): String {
    val fake: Any = FakeInt(42)

    val int1 = 1
    val int42 = 42

    if (fake == int1) return "FakeInt(42) == 1"
    if (fake != int42) return "FakeInt(42) != 42"
    if (int1 == fake) return "1 == FakeInt(42)"
    if (int42 == fake) return "42 == FakeInt(42)"

    return "OK"
}