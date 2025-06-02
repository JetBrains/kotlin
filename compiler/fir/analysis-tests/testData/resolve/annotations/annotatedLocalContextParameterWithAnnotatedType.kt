// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContextParameters

@Target(AnnotationTarget.TYPE, AnnotationTarget.VALUE_PARAMETER)
annotation class Anno(val s: String)

fun f1() {
    class Foo {
        context(@Anno("par") par: @Anno("foo") String)
        val foo: String
            get() = "hello"

        context(@Anno("par") par: @Anno("baz") String)
        fun baz(): String = "hello"
    }
}
