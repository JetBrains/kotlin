package stepOverWhenWithInline

fun main(args: Array<String>) {
    //Breakpoint!
    val prop = 1
    // Break after second
    val a = when {
        1 > 2 -> foo { test(1) }
        2 > 1 -> foo { test(1) }
        else -> foo { test(1) }
    }

    val b = when {
        1 > 2 -> {
            foo { test(1) }
        }
        2 > 1 -> {
            foo { test(1) }
        }
        else -> {
            foo { test(1) }
        }
    }

    val c = when {
        foo { test(1) } > 2 -> 1
        2 > foo { test(1) } -> 2
        else -> foo { test(1) }
    }

    // When with expression
    val a1 = when(prop) {
        2 -> foo { test(1) }
        1 -> foo { test(1) }
        else -> foo { test(1) }
    }

    val b1 = when(prop) {
        2 -> {
            foo { test(1) }
        }
        1 -> {
            foo { test(1) }
        }
        else -> {
            foo { test(1) }
        }
    }

    // Break after first
    val c1 = when(prop) {
        foo { test(1) } -> 1
        foo { test(2) } -> 2
        else -> foo { test(1) }
    }

    val a2 = when {
        2 > 1 -> foo { test(1) }
        1 > 2 -> foo { test(1) }
        else -> foo { test(1) }
    }

    val b2 = when {
        2 > 1 -> {
            foo { test(1) }
        }
        1 > 2 -> {
            foo { test(1) }
        }
        else -> {
            foo { test(1) }
        }
    }

    val c2 = when {
        2 > foo { test(1) } -> 2
        foo { test(1) } > 2 -> 1
        else -> foo { test(1) }
    }
}

inline fun foo(f: () -> Int): Int {
    val a = 1
    return f()
}

fun test(i: Int) = i

// STEP_OVER: 50