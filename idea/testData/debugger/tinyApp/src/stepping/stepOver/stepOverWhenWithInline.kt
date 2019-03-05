package stepOverWhenWithInline

fun main(args: Array<String>) {
    //Breakpoint!
    val prop = 1                                                   // 1
    // Break after second
    val a = when {                                                 // 4
        1 > 2 -> foo { test(1) }                                   // 2
        2 > 1 -> foo { test(1) }                                   // 3
        else -> foo { test(1) }
    }

    val b = when {                                                 // 8
        1 > 2 -> {                                                 // 5
            foo { test(1) }
        }
        2 > 1 -> {                                                 // 6
            foo { test(1) }                                        // 7
        }
        else -> {
            foo { test(1) }
        }
    }

    val c = when {                                                 // 11
        foo { test(1) } > 2 -> 1                                   // 9
        2 > foo { test(1) } -> 2                                   // 10
        else -> foo { test(1) }
    }

    // When with expression
    val a1 = when(1) {                                             // 12 14
        2 -> foo { test(1) }
        1 -> foo { test(1) }                                       // 13
        else -> foo { test(1) }
    }

    val b1 = when(1) {                                             // 15 17
        2 -> {
            foo { test(1) }
        }
        1 -> {
            foo { test(1) }                                        // 16
        }
        else -> {
            foo { test(1) }
        }
    }

    // Break after first
    val c1 = when(1) {                                             // 18 20
        foo { test(1) } -> 1                                       // 19
        foo { test(2) } -> 2
        else -> foo { test(1) }
    }

    val a2 = when {                                                // 22
        2 > 1 -> foo { test(1) }                                   // 21
        1 > 2 -> foo { test(1) }
        else -> foo { test(1) }
    }

    val b2 = when {                                                // 25
        2 > 1 -> {                                                 // 23
            foo { test(1) }                                        // 24
        }
        1 > 2 -> {
            foo { test(1) }
        }
        else -> {
            foo { test(1) }
        }
    }

    val c2 = when {                                                // 27
        2 > foo { test(1) } -> 2                                   // 26
        foo { test(1) } > 2 -> 1
        else -> foo { test(1) }
    }
}                                                                  // 28

inline fun foo(f: () -> Int): Int {
    val a = 1
    return f()
}

fun test(i: Int) = i

// STEP_OVER: 32