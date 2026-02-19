// DO_NOT_CHECK_SYMBOL_RESTORE_K1
// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE_K2

@Target(AnnotationTarget.TYPE, AnnotationTarget.VALUE_PARAMETER)
annotation class Anno(val position: String)

class Foo {
    @Anno("str")
    context(@Anno("param") parameter1: @Anno("1" + "2") Unresolved, parameter2: List<@Anno("str") Unresolved>)
}

fun foo() {
    class Foo {
        @Anno("str")
        context(@Anno("param") parameter1: @Anno("1" + "2") Unresolved, parameter2: List<@Anno("str") Unresolved>)
    }
}

@Anno("str")
context(@Anno("param") parameter1 : @Anno("1" + "2") Unresolved, parameter2: List<@Anno("str") Unresolved>)
