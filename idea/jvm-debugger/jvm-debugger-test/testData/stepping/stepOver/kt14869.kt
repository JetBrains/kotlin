package test

fun main(args: Array<String>) {
    foo().let {
        it
    }
}

fun foo(): Int {
    //Breakpoint!
    return 1
}

// STEP_OVER: 3