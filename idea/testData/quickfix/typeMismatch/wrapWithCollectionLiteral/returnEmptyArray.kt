// "Replace with 'emptyArray()' call" "true"
// WITH_RUNTIME

fun foo(a: String?): Array<String> {
    val w = a ?: return null<caret>
    return arrayOf(w)
}
