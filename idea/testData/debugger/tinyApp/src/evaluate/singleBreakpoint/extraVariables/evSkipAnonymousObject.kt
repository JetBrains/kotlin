package evSkipAnonymousObject

class A {
    var prop = 1
}

fun main(args: Array<String>) {
    val a1 = A()
    val a2 = A()

    //Breakpoint!
    foo(a1.prop)
    object: T {
        override fun f() {
            foo(a2.prop)
        }
    }
}

trait T {
    fun f() {}
}

fun foo(i: Int) {}

// PRINT_FRAME