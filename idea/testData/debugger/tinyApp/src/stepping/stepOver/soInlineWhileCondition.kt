package soInlineWhileCondition

fun main(args: Array<String>) {
    //Breakpoint!
    var i = 1                                                      // 1
    // inline in while condition (true)
    while (id { i < 2 }) {                                         // 2 4
        i++                                                        // 3
    }

    // inline in while condition (false)
    while (id { false }) {                                         // 5
        bar()
    }
}                                                                  // 6

inline fun id(f: () -> Boolean): Boolean {
    return f()
}

fun bar() {}

// STEP_OVER: 6