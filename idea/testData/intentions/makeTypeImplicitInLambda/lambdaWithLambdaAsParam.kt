public class TestingUse {
    fun test6(funcLitfunc: ((x: Int) -> Int) -> Boolean, innerfunc: (y: Int) -> Int): Unit {
    }
}

fun main() {
    val funcInfunc = TestingUse().test6({<caret>(f: (Int) -> Int): Boolean -> f(5) > 20}, {x -> x + 2})
}
