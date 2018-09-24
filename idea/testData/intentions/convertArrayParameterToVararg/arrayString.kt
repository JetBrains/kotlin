// INTENTION_TEXT: Convert to vararg parameter (may break code)
// DISABLE-ERRORS
fun test(a: Array<String><caret>) {
    a[0] = ""
}