package classObjectFunFromClass

class A {
    fun bar() {
        //Breakpoint!
        foo()
    }

    class object {
        fun foo() {
            val a = 1
        }
    }
}

fun main(args: Array<String>) {
    A().bar()
}
