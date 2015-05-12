// IS_APPLICABLE: false
fun test() {
    class Test{
        fun get() : Int = 0
    }
    val test = Test()
    test.g<caret>et()
}
