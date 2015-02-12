package evLineRange

class A {
    var prop = 1
}

fun main(args: Array<String>) {
    val a1 = A()
    val a2 = A()
    val a3 = A()

    foo(a1.prop)
    val i1 = 1
    //Breakpoint!
    foo(a2.prop)
    val i2 = 1
    foo(a3.prop)
}

fun foo(i: Int) {}

// PRINT_FRAME