// ERROR_POLICY: SYNTAX

// FILE: t.kt


384jfjfj2934829...:::%:ББББ

fun 124gga() {}

val 481gu: Boolean = true

var 981llj): Int = 42

// just check if it is compiled
class **jhghssk

fun foo() { 124gga() }
fun bar() { return 481gu }
fun qux() { 981llj) = 481 }

// FILE: b.kt

fun box(): String {
    try {
        foo()
    } catch (e: IllegalStateException) {
        try {
            bar()
        } catch (e: IllegalStateException) {
            try {
                qux()
            } catch (e: IllegalStateException) {
                return "OK"
            }
        }
    }
    return "FAIL"
}