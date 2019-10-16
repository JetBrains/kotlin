package stepOverIfWithInline

fun main(args: Array<String>) {
    //Breakpoint!
    val prop = 1                                                       // 1
    // False with braces
    val a = if (1 > 2) {                                               // 2 4(?)
        foo { test(1) }
    }
    else {
        foo { test(1) }                                                // 3
    }

    // False without braces
    val b = if (1 > 2)                                                 // 5 7(?)
        foo { test(1) }
    else
        foo { test(1) }                                                // 6

    // One line
    val c = if (1 > 2) foo { test(1) } else foo { test(1) }            // 8

    // Else on next line, false
    val d = if (1 > 2) foo { test(1) }                                 // 9 11
            else foo { test(1) }                                       // 10

    // Else on next line, true
    val e = if (1 < 2) foo { test(1) }                                 // 12
            else foo { test(1) }

    // Inline function call in condition
    val f = if (foo { test(1) } > 2) {                                 // 13 15(?)
        foo { test(1) }
    }
    else {
        foo { test(1) }                                                // 14
    }
}                                                                      // 16

inline fun foo(f: () -> Int): Int {
    val a = 1
    return f()
}

fun test(i: Int) = 1

// STEP_OVER: 17