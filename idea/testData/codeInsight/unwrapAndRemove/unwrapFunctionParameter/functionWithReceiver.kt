// IS_APPLICABLE: false
fun test() {
    val i = 1
    val test = Test()
    foo(test.qux<caret>(i))
}

fun foo(i: Int) {}

class Test {
    fun qux(i: Int) = 1
}
