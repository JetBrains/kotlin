// !DIAGNOSTICS: -UNUSED_EXPRESSION
// FILE: simpleName.kt

package foo

fun test() {
    <!UNRESOLVED_REFERENCE!>foo::test<!>
}

// FILE: qualifiedName.kt

package foo.bar

fun test() {
    <!UNRESOLVED_REFERENCE!>foo.bar::test<!>
}
