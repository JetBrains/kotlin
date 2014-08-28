package frameSharedVar

fun main(args: Array<String>) {
    var var1 = 1
    foo {
        //Breakpoint!
        var1 = 2
    }
}

fun foo(f: () -> Unit) {
    f()
}

// PRINT_FRAME