// IS_APPLICABLE: false
// WITH_RUNTIME
package pr442.kotlin

fun foo() {
    <caret>assert(true, "")
}

fun assert(b: Boolean, s: String) {}