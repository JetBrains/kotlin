// !DIAGNOSTICS: -UNUSED_PARAMETER
import Host.suspendFromObject

suspend fun suspendHere() = 1
suspend fun <T> another(a: T) = 1

object Host {
    suspend fun suspendFromObject() = 1
}

fun <T> builder(c: suspend () -> Unit) { }

inline fun run(x: () -> Unit) {}

inline fun runCross(crossinline x: () -> Unit) {}

fun noinline(x: () -> Unit) {}

fun foo() {
    var result = 1
    builder<String> {
        suspendHere()
        suspendFromObject()
        
        another("")
        another(1)

        result += suspendHere()
        result += suspendFromObject()

        run {
            result += suspendHere()
            result += suspendFromObject()
            
            run {
                suspendHere()
                suspendFromObject()
            }
        }

        runCross {
            result += <!NON_LOCAL_SUSPENSION_POINT!>suspendHere<!>()
            result += <!NON_LOCAL_SUSPENSION_POINT!>suspendFromObject<!>()
            runCross {
                <!NON_LOCAL_SUSPENSION_POINT!>suspendHere<!>()
                <!NON_LOCAL_SUSPENSION_POINT!>suspendFromObject<!>()
            }
        }

        noinline {
            result += <!NON_LOCAL_SUSPENSION_POINT!>suspendHere<!>()
            result += <!NON_LOCAL_SUSPENSION_POINT!>suspendFromObject<!>()
            noinline {
                <!NON_LOCAL_SUSPENSION_POINT!>suspendHere<!>()
                <!NON_LOCAL_SUSPENSION_POINT!>suspendFromObject<!>()
            }
        }

        class A {
            fun bar() {
                <!NON_LOCAL_SUSPENSION_POINT!>suspendHere<!>()
                <!NON_LOCAL_SUSPENSION_POINT!>suspendFromObject<!>()
            }
        }

        object : Any() {
            fun baz() {
                <!NON_LOCAL_SUSPENSION_POINT!>suspendHere<!>()
                <!NON_LOCAL_SUSPENSION_POINT!>suspendFromObject<!>()
            }
        }

        builder<Int> {
            suspendHere()
            suspendFromObject()

            another(1)
            another("")
        }
    }
}
