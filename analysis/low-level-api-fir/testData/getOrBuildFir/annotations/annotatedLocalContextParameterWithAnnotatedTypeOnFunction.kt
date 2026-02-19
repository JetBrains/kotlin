// LOOK_UP_FOR_ELEMENT_OF_TYPE: KtAnnotationEntry

@Target(AnnotationTarget.TYPE, AnnotationTarget.VALUE_PARAMETER)
annotation class Anno(val s: String)

fun f1() {
    class Foo {
        context(@Anno("par") par: <expr>@Anno("foo")</expr> String)
        fun bar(): String = "hello"
    }
}
