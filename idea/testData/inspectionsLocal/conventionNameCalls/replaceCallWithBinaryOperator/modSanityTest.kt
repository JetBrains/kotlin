// PROBLEM: none

fun test() {
    class Test {
        operator fun mod(a: Int): Test = Test()
    }
    val test = Test()
    test.<caret>mod(1)
}
