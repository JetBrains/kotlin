package soSimpleInlineIfCondition

fun main(args: Array<String>) {
    //Breakpoint!
    if (foo {
        test(2)
    }) {
        bar()
    }

    bar()
}

inline fun foo(f: () -> Boolean): Boolean = f()

fun test(i: Int): Boolean = true

fun bar() {}
