// "Replace with 'emptySequence()' call" "true"
// WITH_RUNTIME

fun foo(a: String?): Sequence<String> {
    val w = a ?: return null<caret>
    return sequenceOf(w)
}
