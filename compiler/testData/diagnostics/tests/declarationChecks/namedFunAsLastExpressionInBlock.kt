// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_ANONYMOUS_PARAMETER -UNUSED_VARIABLE
// !CHECK_TYPE
// !COMPILER_VERSION 2.0
fun foo(block: () -> (() -> Int)) {}

fun test() {
    val x = fun <!ANONYMOUS_FUNCTION_WITH_NAME!>named1<!>(x: Int): Int { return 1 }
    x checkType { _<Function1<Int, Int>>() }

    foo <!TYPE_MISMATCH!>{ <!EXPECTED_TYPE_MISMATCH("() -> Int")!>fun named2(): Int {return 1}<!> }<!>
    foo(<!TYPE_MISMATCH!>{ <!EXPECTED_TYPE_MISMATCH!>fun named3() = 1<!> }<!>)

    val x1 =
    <!INVALID_IF_AS_EXPRESSION!>if<!> (1 == 1)
    // TODO: Diagnostic content could be better
    <!SYNTAX!><!>fun named4(): Int {return 1}
    <!SYNTAX!>else<!>
    fun named5() = 1

    val x2 =
    <!INVALID_IF_AS_EXPRESSION!>if<!> (1 == 1) {
        fun named6(): Int {
            return 1
        }
    }
    else
    <!SYNTAX!><!>fun named7() = 1

    val x3 = when (1) {
        0 -> fun <!ANONYMOUS_FUNCTION_WITH_NAME!>named8<!>(): Int {return 1}
        else -> fun <!ANONYMOUS_FUNCTION_WITH_NAME!>named9<!>() = 1
    }

    val x31 = when (1) {
        0 -> {
            fun named10(): Int {return 1}
        }
        else -> fun <!ANONYMOUS_FUNCTION_WITH_NAME!>named11<!>() = 1
    }

    val x4 = {
        y: Int -> fun named12(): Int {return 1}
    }

    x4 checkType { _<Function1<Int, Unit>>() }

    { y: Int -> fun named14(): Int {return 1} }
    val b = (fun <!ANONYMOUS_FUNCTION_WITH_NAME!>named15<!>(): Boolean { return true })()

    baz(fun <!ANONYMOUS_FUNCTION_WITH_NAME!>named16<!>(){})
}

fun bar() = fun <!ANONYMOUS_FUNCTION_WITH_NAME!>named<!>() {}

fun <T> run(block: () -> T): T = null!!
fun run2(block: () -> Unit): Unit = null!!
fun baz(obj: Any?) {}

fun success() {
    run { fun named1() = 1 }
    run2 { fun named2() = 1 }

    val x = run { fun named3() = 1 }
    x checkType { _<Unit>() }

    val y = when (1) {
        0 -> {
            fun named4(): Int {return 1}
        }
        else -> {
            fun named5(): Int {return 1}
        }
    }
    y checkType { _<Unit>() }
}
