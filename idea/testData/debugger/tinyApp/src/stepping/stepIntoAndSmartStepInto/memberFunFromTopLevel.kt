package memberFunFromTopLevel

class A {
    fun bar() {
        val a = 1
    }
}

fun main(args: Array<String>) {
    A()

    //Breakpoint!
    A().bar()
}
