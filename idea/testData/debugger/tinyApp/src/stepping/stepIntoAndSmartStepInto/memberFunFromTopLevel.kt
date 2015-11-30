package memberFunFromTopLevel

class A {
    fun bar() {
        val a = 1
    }
}

fun main(args: Array<String>) {
    val a = A()

    //Breakpoint!
    a.bar()
}
