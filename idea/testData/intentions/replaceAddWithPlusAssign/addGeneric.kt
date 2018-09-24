// IS_APPLICABLE: false

fun <C : MutableCollection<Int>> foo(col: C) {
    col.<caret>add(0)
}