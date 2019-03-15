// "Wrap element with 'setOf()' call" "true"
// WITH_RUNTIME

fun foo(a: String) {
    bar(a<caret>)
}

fun bar(a: Set<String>) {}
