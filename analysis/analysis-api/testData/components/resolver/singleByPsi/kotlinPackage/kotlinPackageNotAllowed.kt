// FE1.0 can resolve to `kotlin` package when it's not allowed
// COMPILATION_ERRORS
// FILE: a.kt
package kotlin.pckg

fun foo() {
    b<caret>ar()
}

// FILE: b.kt
package kotlin.pckg

fun bar() {
}
