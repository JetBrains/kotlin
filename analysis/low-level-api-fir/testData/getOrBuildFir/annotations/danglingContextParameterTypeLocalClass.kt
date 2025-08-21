@Target(AnnotationTarget.TYPE, AnnotationTarget.VALUE_PARAMETER)
annotation class Anno(val position: String)

fun foo() {
    class Foo {
        @Anno("str")
        context(@Anno("param") parameter1: @Anno("1" + "2") <expr>Unresolved</expr>, parameter2: List<@Anno("str") Unresolved>)
    }
}
