package classObjectFunFromTopLevel

class A {
    companion object {
        fun bar() {
            val a = 1
        }
    }
}

fun main(args: Array<String>) {
    //Breakpoint!
    A.bar()
}
