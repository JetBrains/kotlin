// IS_APPLICABLE: false
fun test() {
    class Test{
        fun contains(a: Int=1, b: Int=2) : Boolean = true
    }
    val test = Test()
    test.contai<caret>ns(b=3)
}
