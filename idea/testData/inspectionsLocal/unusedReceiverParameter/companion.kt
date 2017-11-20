// PROBLEM: none

class Test(val test: Int) {
    companion object

    fun foo() = test
}

// Used
operator fun Test.<caret>Companion.invoke() = Test(1)


fun main(args: Array<String>) {
    val x = Test()
    x.foo()
}