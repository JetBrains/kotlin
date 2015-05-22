package memberFunFromClass

class A {
    fun bar() {
        //Breakpoint!
        foo()
    }

    fun foo() {
        val a = 1
    }
}

fun main(args: Array<String>) {
    A().bar()
}
