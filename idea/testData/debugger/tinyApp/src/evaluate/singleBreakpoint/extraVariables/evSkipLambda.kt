package evSkipLambda

class A {
    var prop = 1
}

fun main(args: Array<String>) {
    val a1 = A()
    val a2 = A()

    //Breakpoint!
    foo(a1.prop)
    l {
        foo(a2.prop)
    }
}

fun foo(i: Int) {}
fun l(f: () -> Unit) {}

// PRINT_FRAME