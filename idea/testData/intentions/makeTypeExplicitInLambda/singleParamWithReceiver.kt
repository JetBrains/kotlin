class TestingUse {
    fun test(sum: Int.(a: Int) -> Int, b: Int): Int {
        return b.sum(b)
    }
}

fun main() {
    val num = TestingUse().test({ <caret>x -> x + 2 }, 20)
}
