// WITH_RUNTIME

fun foo() {
    var range : CharRange = 'a' .. 'z'
    range.<caret>endInclusive
}
