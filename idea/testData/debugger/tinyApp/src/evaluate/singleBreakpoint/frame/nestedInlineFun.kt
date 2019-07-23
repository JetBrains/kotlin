package nestedInlineFun

fun main() {
    val a = 1
    foo {
        val b = 2
        //Breakpoint!
        val c = 0
    }
}

inline fun foo(block: () -> Unit) {
    val x = 3
    bar(1, block)
}

inline fun bar(count: Int, block: () -> Unit) {
    var i = count
    while (i-- > 0) {
        block()
    }
}

// SHOW_KOTLIN_VARIABLES
// PRINT_FRAME