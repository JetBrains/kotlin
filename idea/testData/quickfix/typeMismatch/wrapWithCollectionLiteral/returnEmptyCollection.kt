// "Replace with 'emptyList()' call" "true"
// WITH_RUNTIME

fun foo(a: String?): Collection<String> {
    val w = a ?: return null<caret>
    return listOf(w)
}
