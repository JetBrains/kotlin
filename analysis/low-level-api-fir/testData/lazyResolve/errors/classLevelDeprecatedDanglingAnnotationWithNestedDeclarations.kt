// MEMBER_CLASS_FILTER: org.jetbrains.kotlin.fir.symbols.impl.FirDanglingModifierSymbol
annotation class Anno(val s: String)

class Out<caret>er {
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
}