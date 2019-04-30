package capturedValues2

fun main() {
    1000.foo()
}

fun Int.foo() {
    block {
        val b = 1
        val b2 = 2
        block("x") foo@ {
            //Breakpoint!
            this@foo + b
        }
    }
}

fun block(block: () -> Unit) {
    block()
}

fun <T> block(obj: T, block: T.() -> Unit) {
    obj.block()
}

// SHOW_KOTLIN_VARIABLES
// PRINT_FRAME