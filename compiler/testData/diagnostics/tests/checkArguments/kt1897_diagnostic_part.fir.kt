// !WITH_NEW_INFERENCE
//KT-1897 When call cannot be resolved to any function, save information about types of arguments

package a

fun bar() {}

fun foo(i: Int, s: String) {}

fun test() {

    bar(<!TOO_MANY_ARGUMENTS, UNRESOLVED_REFERENCE!>xx<!>)

    bar <!TOO_MANY_ARGUMENTS!>{ }<!>

    foo("", 1, <!TOO_MANY_ARGUMENTS, UNRESOLVED_REFERENCE!>xx<!>)

    foo(<!NAMED_PARAMETER_NOT_FOUND!>r<!> = <!UNRESOLVED_REFERENCE!>xx<!>, i = "", s = "")

    foo(i = 1, <!ARGUMENT_PASSED_TWICE!>i<!> = 1, s = <!ARGUMENT_TYPE_MISMATCH!>11<!>)

    foo(<!ARGUMENT_TYPE_MISMATCH!>""<!>, s = <!ARGUMENT_TYPE_MISMATCH!>2<!>)

    foo(i = "", s = 2, <!TOO_MANY_ARGUMENTS!>33<!>)

    foo("", 1) <!TOO_MANY_ARGUMENTS!>{}<!>

    foo("", 1) <!TOO_MANY_ARGUMENTS!>{}<!> <!TOO_MANY_ARGUMENTS!>{}<!>
}
