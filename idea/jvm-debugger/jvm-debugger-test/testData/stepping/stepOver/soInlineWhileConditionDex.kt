package soInlineWhileConditionDex

fun main(args: Array<String>) {
    //Breakpoint!
    var i = 1
    // inline in while condition (true)
    while (id { i < 2 }) {
        i++
    }

    // inline in while condition (false)
    while (id { false }) {
        bar()
    }
}

inline fun id(f: () -> Boolean): Boolean {
    return f()
}

fun bar() {}

// STEP_OVER: 12