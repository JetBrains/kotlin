package frameObject

fun main(args: Array<String>) {
    foo {
        //Breakpoint!
        O.obProp
    }
}

object O {
    val obProp = 1
}

fun foo(f: () -> Unit) {
    f()
}

// PRINT_FRAME