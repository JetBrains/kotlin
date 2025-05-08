// LOOK_UP_FOR_ELEMENT_OF_TYPE: org.jetbrains.kotlin.psi.KtAnnotationEntry
@Target(AnnotationTarget.TYPE, AnnotationTarget.VALUE_PARAMETER)
annotation class Anno(val position: String)

fun foo() {
    class Foo {
        @Anno("str")
        context(@Anno("param") parameter1: <expr>@Anno("1" + "2")</expr> Unresolved, parameter2: List<@Anno("str") Unresolved>)
    }
}
