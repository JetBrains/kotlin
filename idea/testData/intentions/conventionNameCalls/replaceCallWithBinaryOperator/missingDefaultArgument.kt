// IS_APPLICABLE: false
fun test() {
    class Test{
        fun plus(a: Int=1, b: Int=2) : Int = 0
    }
    val test = Test()
    test.p<caret>lus(b=3)
}
