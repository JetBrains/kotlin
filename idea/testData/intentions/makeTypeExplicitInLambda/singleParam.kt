class TestingUse {
    fun test3(double: (a: Int) -> Int, b: Int): Int {
        return double(b)
    }
}

fun main() {
    val num = TestingUse().test3({<caret>x -> 2*x}, 20)
}
