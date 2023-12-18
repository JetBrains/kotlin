// !LANGUAGE: +DisableCompatibilityModeForNewInference
// SKIP_TXT
// FULL_JDK

fun <T> bar(action: () -> T): T = action()
fun bar(action: java.lang.Runnable) { }

fun foo(): String = ""

fun main() {
    val x = bar() { foo() } // OK with default current 1.5/1.6, Error with DisableCompatibilityModeForNewInference enabled, Ok in K2
    x.length
}
