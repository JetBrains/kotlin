package inlineInIfTrue

fun main(args: Array<String>) {
    val bar = ""
    //Breakpoint!
    if (inlineCall { true }) {
        foo()
    }
}

fun foo() {}

inline fun inlineCall(predicate: (String?) -> Boolean): Boolean {
    return true
}