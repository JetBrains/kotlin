annotation class Anno(val s: String)

@Deprecated("FirstClass")
@Anno("FirstClass")
class Fir<caret>stClass @Deprecated("constructor") @Anno("constructor") constructor(@Deprecated("constructorProperty") @Anno("constructorProperty") val a: Int) {
    @Deprecated("memberFunction")
    @Anno("memberFunction")
    fun memberFunction() {
    }

    @Deprecated("memberProperty")
    @Anno("memberProperty")
    val memberProperty = 32

    @Deprecated("NestedClass")
    @Anno("NestedClass")
    class NestedClass @Deprecated("constructor") @Anno("constructor") constructor(@Deprecated("constructorProperty") @Anno("constructorProperty") val a: Int) {
        @Deprecated("memberFunction")
        @Anno("memberFunction")
        fun memberFunction() {
        }

        @Deprecated("memberProperty")
        @Anno("memberProperty")
        val memberProperty = 32
    }

    @Deprecated("companion")
    @Anno("companion")
    companion object {
        @Deprecated("memberFunction")
        @Anno("memberFunction")
        fun memberFunction() {
        }

        @Deprecated("memberProperty")
        @Anno("memberProperty")
        val memberProperty = 32
    }
}

@Deprecated("AnotherClass")
@Anno("AnotherClass")
class AnotherClass {
    @Deprecated("memberFunction")
    @Anno("memberFunction")
    fun memberFunction() {
    }
}