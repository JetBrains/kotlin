// IS_APPLICABLE: false
// ERROR: Cannot find a parameter with this name: c
fun test() {
    class Test{
        fun get(a: Int=1, b: Int=2) : Int = 0
    }
    val test = Test()
    test.g<caret>et(c=3)
}
