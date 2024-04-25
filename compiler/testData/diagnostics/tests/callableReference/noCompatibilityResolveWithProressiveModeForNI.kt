// FIR_IDENTICAL
// LANGUAGE: +DisableCompatibilityModeForNewInference
// DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_EXPRESSION

fun bar(): Int = 0

object Scope {
    fun <T> foo(f: () -> T): T = f()

    fun bar(x: Int = 0): String = ""

    fun test() {
        val r1 = foo(::bar)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>r1<!>

        val r2 = foo(Scope::bar)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>r2<!>
    }
}