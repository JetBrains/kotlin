// FIR_IDENTICAL
annotation class TestAnnWithIntArray(val x: IntArray)
annotation class TestAnnWithStringArray(val x: Array<String>)

@TestAnnWithIntArray(intArrayOf(1, 2, 3))
@TestAnnWithStringArray(arrayOf("a", "b", "c"))
fun test1() {}

@TestAnnWithIntArray([4, 5, 6])
@TestAnnWithStringArray(["d", "e", "f"])
fun test2() {}