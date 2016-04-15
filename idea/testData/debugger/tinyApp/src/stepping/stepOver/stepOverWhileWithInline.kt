package stepOverWhileWithInline

fun main(args: Array<String>) {
    //Breakpoint!
    var prop = 0
    // inline on last line of while
    while(prop < 1) {
        prop++
        foo { test(1) }
    }

    // inline call inside while
    while(prop < 2) {
        foo { test(1) }
        prop++
    }

    do {
        prop++
        foo { test(1) }
    } while(prop < 3)

    do {
        foo { test(1) }
        prop++
    } while(prop < 4)

    var i = 1
    // inline in while condition (true)
    while(foo { test(i++) } == 1) {
        prop++
    }

    // inline in while condition (false)
    while(foo { test(2) } == 1) {
        prop++
    }
}

inline fun foo(f: () -> Int): Int {
    val a = 1
    return f()
}

fun test(i: Int) = i

// STEP_OVER: 41