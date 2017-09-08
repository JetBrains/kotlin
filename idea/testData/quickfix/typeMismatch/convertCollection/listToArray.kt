// "Convert expression to 'Array' by inserting '.toTypedArray()'" "true"
// WITH_RUNTIME

fun foo(a: List<String>) {
    bar(a<caret>)
}

fun bar(a: Array<String>) {}