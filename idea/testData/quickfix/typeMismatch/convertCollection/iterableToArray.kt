// "Convert expression to 'Array' by inserting '.toList().toTypedArray()'" "true"
// WITH_RUNTIME

fun foo(a: Iterable<String>) {
    bar(a<caret>)
}

fun bar(a: Array<String>) {}