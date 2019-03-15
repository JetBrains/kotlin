// "Wrap element with 'listOf()' call" "true"
// WITH_RUNTIME

fun foo() {
    bar(null<caret>)
}

fun bar(a: List<String?>) {}
