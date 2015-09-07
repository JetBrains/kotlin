package stepOutInlineFunction

fun main(args: Array<String>) {
    foo {
        test(2)
    }
    test(3)
}

inline fun foo(f: () -> Unit) {
    //Breakpoint!
    val a = 1
    f()
    val b = 2
}

fun test(i: Int) = 1

// STEP_OUT: 2