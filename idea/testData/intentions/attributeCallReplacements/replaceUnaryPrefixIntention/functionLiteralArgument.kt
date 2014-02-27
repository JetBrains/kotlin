// IS_APPLICABLE: false
fun test() {
    class Test {
        fun plus(fn: () -> Unit): Test = Test()
    }
    val test = Test()
    test.pl<caret>us {}
}
