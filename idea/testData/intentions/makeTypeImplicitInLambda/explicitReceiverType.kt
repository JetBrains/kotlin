public class TestingUse {
    fun test(sum: Int.(a: Int) -> Int, b: Int): Int {
        return b.sum(b)
    }
}

fun main() {
    val num = TestingUse().test({ Int.(x: Int): Int -> x + 2 <caret>}, 20)
}