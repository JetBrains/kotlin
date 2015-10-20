// IS_APPLICABLE: false
fun test() {
    class Test{
        operator fun get(a: Int = 0, b: Int = 1, c: Int = 2, d: Int = 3) : Int = 0
    }
    val test = Test()
    test.g<caret>et(1, c=3, b=2)
}
