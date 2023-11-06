// FILE: a.kt
package kotlinx.pckg

fun foo() {
    b<caret>ar()
}

// FILE: b.kt
package kotlinx.pckg

fun bar() {
}
