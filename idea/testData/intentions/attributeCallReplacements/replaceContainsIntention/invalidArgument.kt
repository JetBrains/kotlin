// IS_APPLICABLE: false
// ERROR: Cannot find a parameter with this name: c
fun test() {
    class Test{
        fun contains(a: Int=1, b: Int=2): Boolean = true
    }
    val test = Test()
    test.c<caret>ontains(c=3)
}
