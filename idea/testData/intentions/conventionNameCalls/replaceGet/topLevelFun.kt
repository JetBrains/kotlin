// IS_APPLICABLE: false
package p

fun get(s: String) = s

fun foo() {
    p.<caret>get("x")
}
