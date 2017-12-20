// FIX: Replace with '%' operator

fun test() {
    class Test {
        operator fun rem(a: Int): Test = Test()
    }
    val test = Test()
    test.<caret>rem(1)
}
