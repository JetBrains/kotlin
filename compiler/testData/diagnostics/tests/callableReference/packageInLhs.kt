// !DIAGNOSTICS: -UNUSED_EXPRESSION
// FILE: simpleName.kt

package foo

fun test() {
    <!EXPRESSION_EXPECTED_PACKAGE_FOUND!>foo<!>::<!DEBUG_INFO_MISSING_UNRESOLVED!>test<!>
    <!EXPRESSION_EXPECTED_PACKAGE_FOUND!>foo<!>::class
}

// FILE: qualifiedName.kt

package foo.bar

fun test() {
    foo.<!EXPRESSION_EXPECTED_PACKAGE_FOUND!>bar<!>::<!DEBUG_INFO_MISSING_UNRESOLVED!>test<!>
    foo.<!EXPRESSION_EXPECTED_PACKAGE_FOUND!>bar<!>::class
}
