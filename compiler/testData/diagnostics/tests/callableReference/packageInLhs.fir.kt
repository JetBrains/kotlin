// !DIAGNOSTICS: -UNUSED_EXPRESSION
// FILE: simpleName.kt

package foo

fun test() {
    <!EXPRESSION_EXPECTED_PACKAGE_FOUND!>foo<!>::test
    <!EXPRESSION_EXPECTED_PACKAGE_FOUND!>foo<!>::class
}

// FILE: qualifiedName.kt

package foo.bar

fun test() {
    foo.<!EXPRESSION_EXPECTED_PACKAGE_FOUND!>bar<!>::test
    foo.<!EXPRESSION_EXPECTED_PACKAGE_FOUND!>bar<!>::class
}
