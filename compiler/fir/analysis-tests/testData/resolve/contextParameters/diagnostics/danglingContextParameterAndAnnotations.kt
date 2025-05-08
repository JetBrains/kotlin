// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters

@Target(AnnotationTarget.TYPE, AnnotationTarget.VALUE_PARAMETER)
annotation class Anno(val position: String)

class Foo {
    @Anno("str")
    context(@Anno("param") parameter1: @Anno("1" + "2") Unresolved, parameter2: List<@Anno("str") Unresolved>)<!SYNTAX!><!>
}

fun foo() {
    class Foo {
        @Anno("str")
        context(@Anno("param") parameter1: @Anno("1" + "2") Unresolved, parameter2: List<@Anno("str") Unresolved>)<!SYNTAX!><!>
    }
}

@Anno("str")
<!UNSUPPORTED!>context(@Anno("param") parameter1 : @Anno("1" + "2") Unresolved, parameter2: List<@Anno("str") Unresolved>)<!><!SYNTAX!><!>
