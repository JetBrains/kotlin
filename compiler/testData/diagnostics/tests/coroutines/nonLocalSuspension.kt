// !DIAGNOSTICS: -UNUSED_PARAMETER

suspend fun suspendHere() = 1
suspend fun <T> another(a: T) = 1

fun <T> builder(c: suspend () -> Unit) { }

inline fun run(x: () -> Unit) {}

inline fun runCross(crossinline x: () -> Unit) {}

fun noinline(x: () -> Unit) {}

fun foo() {
    var result = 1
    builder<String> {
        suspendHere()
        another("")
        another(1)

        result += suspendHere()

        run {
            result += suspendHere()
            run {
                suspendHere()
            }
        }

        runCross {
            result += <!NON_LOCAL_SUSPENSION_POINT!>suspendHere<!>()
            runCross {
                <!NON_LOCAL_SUSPENSION_POINT!>suspendHere<!>()
            }
        }

        noinline {
            result += <!NON_LOCAL_SUSPENSION_POINT!>suspendHere<!>()
            noinline {
                <!NON_LOCAL_SUSPENSION_POINT!>suspendHere<!>()
            }
        }

        class A {
            fun bar() {
                <!NON_LOCAL_SUSPENSION_POINT!>suspendHere<!>()
            }
        }

        object : Any() {
            fun baz() {
                <!NON_LOCAL_SUSPENSION_POINT!>suspendHere<!>()
            }
        }

        builder<Int> {
            suspendHere()

            another(1)
            another("")
        }
    }
}
