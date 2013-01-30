//KT-1897 When call cannot be resolved to any function, save information about types of arguments

package a

fun bar() {}

fun foo(<!UNUSED_PARAMETER!>i<!>: Int, <!UNUSED_PARAMETER!>s<!>: String) {}

fun test() {

    bar(<!TOO_MANY_ARGUMENTS, UNRESOLVED_REFERENCE!>xx<!>)

    bar <!TOO_MANY_ARGUMENTS, DANGLING_FUNCTION_LITERAL_ARGUMENT_SUSPECTED!>{ }<!>

    foo(<!TYPE_MISMATCH!>""<!>, <!ERROR_COMPILE_TIME_VALUE!>1<!>, <!TOO_MANY_ARGUMENTS, UNRESOLVED_REFERENCE!>xx<!>)

    foo(<!NAMED_PARAMETER_NOT_FOUND!>r<!> = <!UNRESOLVED_REFERENCE!>xx<!>, i = <!TYPE_MISMATCH!>""<!>, s = "")

    foo(i = 1, <!ARGUMENT_PASSED_TWICE!>i<!> = 1, s = <!ERROR_COMPILE_TIME_VALUE!>11<!>)

    foo(<!TYPE_MISMATCH!>""<!>, s = <!ERROR_COMPILE_TIME_VALUE!>2<!>)

    foo(i = <!TYPE_MISMATCH!>""<!>, s = <!ERROR_COMPILE_TIME_VALUE!>2<!>, <!MIXING_NAMED_AND_POSITIONED_ARGUMENTS!>33<!>)

    foo(<!TYPE_MISMATCH!>""<!>, <!ERROR_COMPILE_TIME_VALUE!>1<!>) <!TOO_MANY_ARGUMENTS!>{}<!>

    foo(<!TYPE_MISMATCH!>""<!>, <!ERROR_COMPILE_TIME_VALUE!>1<!>) <!TOO_MANY_ARGUMENTS!>{}<!> <!MANY_FUNCTION_LITERAL_ARGUMENTS!>{}<!>
}