// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_ANONYMOUS_PARAMETER -UNUSED_VARIABLE
// !CHECK_TYPE
fun foo(block: () -> (() -> Int)) {}

fun test() {
    val x = fun <!ANONYMOUS_FUNCTION_WITH_NAME!>named1<!>(x: Int): Int { return 1 }
    x checkType { _<Function1<Int, Int>>() }

    foo { <!EXPECTED_TYPE_MISMATCH(\(\) -> Int)!>fun named2(): Int {return 1}<!> }
    foo({ <!EXPECTED_TYPE_MISMATCH!>fun named3() = 1<!> })

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
        0 -> <!EXPECTED_TYPE_MISMATCH!>fun named8(): Int {return 1}<!>
        else -> <!EXPECTED_TYPE_MISMATCH!>fun named9() = 1<!>
    }

    val x31 = when (1) {
        0 -> {
            <!EXPECTED_TYPE_MISMATCH!>fun named10(): Int {return 1}<!>
        }
        else -> <!EXPECTED_TYPE_MISMATCH!>fun named11() = 1<!>
    }

    val x4 = {
        y: Int -> fun named12(): Int {return 1}
    }

    x4 checkType { _<Function1<Int, Unit>>() }

    <!UNUSED_LAMBDA_EXPRESSION!>{ y: Int -> fun named14(): Int {return 1} }<!>
}

fun <T> run(block: () -> T): T = null!!
fun run2(block: () -> Unit): Unit = null!!

fun success() {
    run { fun named1() = 1 }
    run2 { fun named2() = 1 }

    val x = run { fun named3() = 1 }
    x checkType { _<Unit>() }
}
