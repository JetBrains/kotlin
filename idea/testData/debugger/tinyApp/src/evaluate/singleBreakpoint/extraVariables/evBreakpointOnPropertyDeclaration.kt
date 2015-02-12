package evBreakpointOnPropertyDeclaration

class A {
    var prop = 1
}

fun main(args: Array<String>) {
    val a1 = A()
    val a2 = A()
    val a3 = A()

    val p1 = a1.prop
    //Breakpoint!
    val p2 = a2.prop
    val p3 = a3.prop
}

// PRINT_FRAME