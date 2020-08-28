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
            result += suspendHere()
            result += suspendFromObject()
            runCross {
                suspendHere()
                suspendFromObject()
            }
        }

        noinline {
            result += suspendHere()
            result += suspendFromObject()
            noinline {
                suspendHere()
                suspendFromObject()
            }
        }

        class A {
            fun bar() {
                suspendHere()
                suspendFromObject()
            }
        }

        object : Any() {
            fun baz() {
                suspendHere()
                suspendFromObject()
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
