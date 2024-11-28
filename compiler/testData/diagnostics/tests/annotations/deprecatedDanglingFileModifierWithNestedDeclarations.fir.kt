// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-72740
// FIR_PARSER: Psi

annotation class Anno(val s: String)

@Deprecated("Use 'AAA' instead"
open class MyClass : Any() {
    val foo = 24

    @Anno("str")
    fun baz() {

    }

    companion object {
        @Anno("something")
        fun getSomething(a: Int = 24) {

        }
    }
}
