// PROBLEM: none
fun test() {
    class Test{
        operator fun get(a: Int=1, b: Int=2) : Int = 0
    }
    val test = Test()
    test.g<caret>et(b=3)
}
