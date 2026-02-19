// RESOLVE_DANGLING_MODIFIER
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