data class TestData(val first: Int, val second: String?)

fun f() : TestData {}

fun test(b: String?, p: Int) {
    if (p > 0) {
        val (a, b) = f()
        print(b!!.size)
    }
    else {
        print(b!!.size)
    }
    <caret>b
}
