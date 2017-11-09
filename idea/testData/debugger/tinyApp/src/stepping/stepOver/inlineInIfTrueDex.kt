package inlineInIfTrueDex

fun main(args: Array<String>) {
    val bar = ""
    //Breakpoint!
    if (inlineCall { true }) {                                    // 1
        foo()                                                     // 2
    }
}                                                                 // 3

fun foo() {}

inline fun inlineCall(predicate: (String?) -> Boolean): Boolean {
    return true
}

// STEP_OVER: 6