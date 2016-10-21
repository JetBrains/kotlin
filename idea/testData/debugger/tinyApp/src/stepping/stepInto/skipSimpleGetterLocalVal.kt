package skipSimpleGetterLocalVal

fun main(args: Array<String>) {
    class Test {
        val a2 = 12
    }

    val t = Test()

    val a1 = 1

    //Breakpoint!
    a1 + t.a2                    // 1

    foo()                        // 2
}

fun foo() {}

// SKIP_GETTERS: true