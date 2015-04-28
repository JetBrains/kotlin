public class TestingUse {
    fun test5(coerced: (x: Int) -> Unit, a: Int): Unit {
        return coerced(5)
    }
}

fun main() {
    val coercion = TestingUse().test5({<caret>x -> x + 2}, 20)
}
