import kotlin.test.*



fun box() {
    val data = "abcd".toList()
    val result = data.joinToString("_", "(", ")")
    assertEquals("(a_b_c_d)", result)

    val data2 = "verylongstring".toList()
    val result2 = data2.joinToString("-", "[", "]", 11, "oops")
    assertEquals("[v-e-r-y-l-o-n-g-s-t-r-oops]", result2)

    val data3 = "a1/b".toList()
    val result3 = data3.joinToString() { it.toUpperCase().toString() }
    assertEquals("A, 1, /, B", result3)
}
