// "Replace with 'emptySet()' call" "true"
// WITH_RUNTIME

fun foo(a: String?): Set<String> {
    val w = a ?: return null<caret>
    return setOf(w)
}
