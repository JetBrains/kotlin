// RESOLVE_DANGLING_MODIFIER
// ISSUE: KT-72740
annotation class Anno(val s: String)

@Anno("Use 'AAA' instead"
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