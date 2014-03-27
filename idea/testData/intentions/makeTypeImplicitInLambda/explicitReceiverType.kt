public class TestingUse {
    fun test(sum: Int.(a: Int) -> Int, b: Int): Int {
        return b.sum(b)
    }

    fun test2(sum: (a: Int, b: Int) -> Int, c: Int): Int {
        return sum(c, 5)
    }

    fun test3(double: (a: Int) -> Int, b: Int): Int {
        return double(b)
    }
}

fun main() {
    val num = TestingUse().test({ <caret>Int.(x: Int): Int -> x + 2 }, 20)
}