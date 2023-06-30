class A {
    fun <caret>x(): Int {
        return doSmth("str")
    }
}

fun doSmth(i: String) = 4