// !DIAGNOSTICS: -UNUSED_EXPRESSION
// FILE: simpleName.kt

package foo

fun test() {
    foo::test
}

// FILE: qualifiedName.kt

package foo.bar

fun test() {
    foo.bar::test
}
