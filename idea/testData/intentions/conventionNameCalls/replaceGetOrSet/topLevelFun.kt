// IS_APPLICABLE: false
// ERROR: 'operator' modifier is inapplicable on this function: must be a member or an extension function

package p

operator fun get(s: String) = s

fun foo() {
    p.<caret>get("x")
}
