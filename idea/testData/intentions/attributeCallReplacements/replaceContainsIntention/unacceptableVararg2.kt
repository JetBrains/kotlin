// IS_APPLICABLE: false
fun test() {
    class Test{
        fun contains(vararg b: Int, c: Int = 0): Boolean = true
    }
    val test = Test()
    test.contains<caret>(0, 1)
}
