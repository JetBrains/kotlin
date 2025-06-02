// BODY_RESOLVE
// LANGUAGE: +ContextParameters

@Target(AnnotationTarget.TYPE)
annotation class Anno(val s: String)

fun f<caret>1() {
    class Foo {
        context(par: @Anno("foo") String)
        val foo: String
            get() = "hello"

        context(par: @Anno("baz") String)
        fun baz(): String = "hello"
    }
}
