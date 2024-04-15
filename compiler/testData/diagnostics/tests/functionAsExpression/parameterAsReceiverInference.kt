// ISSUE: KT-58966

fun <T> execute(block: T.() -> Unit) {}
fun acceptString(arg: String) {}

fun foo1() {
    execute<String>(
        fun(arg: String) {
            // K1 & K2: ok
            acceptString(arg)
        }
    )
}

fun foo2() {
    execute<String>(
        fun(arg) {
            // K1: TYPE_MISMATCH (String expected, Any? inferred)
            // K2: ok
            acceptString(<!TYPE_MISMATCH!>arg<!>)
        }
    )
}

fun <T, F> execute2(block: T.(F) -> Unit) {}
fun acceptStringAndInt(arg1: String, arg2: Int) {}

fun foo3() {
    execute2<String, Int>(
        fun(arg1: String, arg2: Int) {
            // K1 & K2: ok
            acceptStringAndInt(arg1, arg2)
        }
    )
}

fun foo4() {
    execute2<String, Int>(
        <!TYPE_MISMATCH!>fun(arg1, arg2) {
            // K1: TYPE_MISMATCH (String expected, Any? inferred)
            // K2: ok
            acceptStringAndInt(<!TYPE_MISMATCH!>arg1<!>, arg2)
        }<!>
    )
}
