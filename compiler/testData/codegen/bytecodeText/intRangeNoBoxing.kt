fun Int.until(other: Int) = this..other - 1
fun foo() {
    val range = 1 until 2
    for (i in range) {}
}

// 1 INVOKEVIRTUAL kotlin/ranges/IntRange.getFirst \(\)I
// 1 getFirst
// 1 INVOKEVIRTUAL kotlin/ranges/IntRange.getLast \(\)I
// 1 getLast