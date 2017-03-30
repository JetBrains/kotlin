// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_ANONYMOUS_PARAMETER -UNUSED_VARIABLE
// !CHECK_TYPE
fun foo(block: () -> (() -> Int)) {}

fun test() {
    foo { fun(): Int {return 1} }
    foo({ fun() = 1 })

    val x1 =
        if (1 == 1)
            fun(): Int {return 1}
        else
            fun() = 1

    val x2 =
            if (1 == 1) {
                fun(): Int {
                    return 1
                }
            }
            else
                fun() = 1

    val x3 = when (1) {
        0 -> fun(): Int {return 1}
        else -> fun() = 1
    }

    val x31 = when (1) {
        0 -> {
            fun(): Int {return 1}
        }
        else -> fun() = 1
    }

    val x4 = {
        y: Int -> fun(): Int {return 1}
    }

    x4 checkType { _<Function1<Int, Function0<Int>>>() }

    <!UNUSED_LAMBDA_EXPRESSION!>{ y: Int -> fun(): Int {return 1} }<!>
}
