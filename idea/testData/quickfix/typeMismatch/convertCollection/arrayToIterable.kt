// "Convert expression to 'Iterable' by inserting '.toList()'" "true"
// WITH_RUNTIME

fun foo(a: Array<String>) {
    bar(a<caret>)
}

fun bar(a: Iterable<String>) {}