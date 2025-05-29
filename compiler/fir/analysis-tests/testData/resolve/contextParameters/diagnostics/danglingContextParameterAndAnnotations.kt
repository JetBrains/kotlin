// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters

@Target(AnnotationTarget.TYPE, AnnotationTarget.VALUE_PARAMETER)
annotation class Anno(val position: String)

class Foo {
    @Anno("str")
    context(@Anno("param") parameter1: @Anno("1" + "2") <!UNRESOLVED_REFERENCE!>Unresolved<!>, parameter2: List<@Anno("str") <!UNRESOLVED_REFERENCE!>Unresolved<!>>)<!SYNTAX!><!>
}

fun foo() {
    class Foo {
        @Anno("str")
        context(@Anno("param") parameter1: @Anno("1" + "2") <!UNRESOLVED_REFERENCE!>Unresolved<!>, parameter2: List<@Anno("str") <!UNRESOLVED_REFERENCE!>Unresolved<!>>)<!SYNTAX!><!>
    }
}

@Anno("str")
context(@Anno("param") parameter1 : @Anno("1" + "2") <!UNRESOLVED_REFERENCE!>Unresolved<!>, parameter2: List<@Anno("str") <!UNRESOLVED_REFERENCE!>Unresolved<!>>)<!SYNTAX!><!>
