package classObjectFunFromTopLevel

class A {
    default object {
        fun bar() {
            val a = 1
        }
    }
}

fun main(args: Array<String>) {
    //Breakpoint!
    A.bar()
}
