// "Convert expression to 'List' by inserting '.toList()'" "true"
// WITH_RUNTIME

fun foo(a: Sequence<String>) {
    bar(a<caret>)
}

fun bar(a: List<String>) {}