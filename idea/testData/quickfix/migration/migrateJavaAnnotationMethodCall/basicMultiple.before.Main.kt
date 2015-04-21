// "Replace method call with property access" "true"
// WITH_RUNTIME

fun foo(ann: Ann) {
    ann.value()<caret>
}
