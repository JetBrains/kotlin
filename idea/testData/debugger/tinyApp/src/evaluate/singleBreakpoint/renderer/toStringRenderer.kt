package toStringRenderer

fun main(args: Array<String>) {
    val a = A()
    //Breakpoint!
    args.size()
}

class A {
    override fun toString() = "myA"
}

// PRINT_FRAME