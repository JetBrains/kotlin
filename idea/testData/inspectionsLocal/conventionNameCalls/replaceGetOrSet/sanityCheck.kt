// PROBLEM: none
// ERROR: Unresolved reference: got
fun test() {
    class Test{
        operator fun get(i: Int) : Int = 0
    }
    val test = Test()
    test.g<caret>ot(0)
}
