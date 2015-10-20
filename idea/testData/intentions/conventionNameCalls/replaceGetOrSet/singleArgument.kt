// INTENTION_TEXT: Replace 'get' call with indexing operator

fun test() {
    class Test{
        operator fun get(i: Int) : Int = 0
    }
    val test = Test()
    test.g<caret>et(0)
}
