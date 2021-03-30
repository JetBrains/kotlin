// !WITH_NEW_INFERENCE
//KT-1897 When call cannot be resolved to any function, save information about types of arguments

package a

fun bar() {}

fun foo(i: Int, s: String) {}

fun test() {

    <!INAPPLICABLE_CANDIDATE!>bar<!>(<!UNRESOLVED_REFERENCE!>xx<!>)

    <!INAPPLICABLE_CANDIDATE!>bar<!> { }

    <!INAPPLICABLE_CANDIDATE!>foo<!>("", 1, <!UNRESOLVED_REFERENCE!>xx<!>)

    foo(r = <!UNRESOLVED_REFERENCE!>xx<!>, i = <!ARGUMENT_TYPE_MISMATCH!>""<!>, s = "")

    foo(i = 1, i = 1, s = <!ARGUMENT_TYPE_MISMATCH!>11<!>)

    foo(<!ARGUMENT_TYPE_MISMATCH!>""<!>, s = <!ARGUMENT_TYPE_MISMATCH!>2<!>)

    <!INAPPLICABLE_CANDIDATE!>foo<!>(i = "", s = 2, 33)

    <!INAPPLICABLE_CANDIDATE!>foo<!>("", 1) {}

    <!INAPPLICABLE_CANDIDATE!>foo<!>("", 1) {} {}
}
