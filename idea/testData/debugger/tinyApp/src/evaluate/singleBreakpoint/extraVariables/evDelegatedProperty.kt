package evDelegatedProperty

import kotlin.properties.Delegates

class A {
    var prop: Int by Delegates.notNull()
}

fun main(args: Array<String>) {
    val a = A()
    a.prop = 1
    //Breakpoint!
    val b = a.prop
}

// PRINT_FRAME