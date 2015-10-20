// IS_APPLICABLE: false
package p

operator fun get(s: String) = s

fun foo() {
    p.<caret>get("x")
}
