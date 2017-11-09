package soInlineFunOnOneLineForDex

fun main(args: Array<String>) {
    val list = listOf(1, 2, 3)

    list.any1 { it > 2 }

    foo()
}

fun foo() {

}

inline fun <T> Iterable<T>.any1(predicate: (T) -> Boolean): Boolean {
    //Breakpoint!
    for (element in this) if (predicate(element)) return true
    return false
}

// STEP_OVER: 3