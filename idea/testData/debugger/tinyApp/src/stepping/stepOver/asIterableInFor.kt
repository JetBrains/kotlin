package asIterableInFor

fun <T> nonInline(p: T): T = p

fun main(args: Array<String>) {
    //Breakpoint!
    val list = listOf("a", "b", "c")
    for (element in list.asIterable()) {
        nonInline(element)
    }
}

// STEP_OVER: 4

// Tests that last line will be "9", no "11". See KT-13534.
