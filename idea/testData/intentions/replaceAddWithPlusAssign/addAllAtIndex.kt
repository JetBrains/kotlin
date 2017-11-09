// IS_APPLICABLE: false
// WITH_RUNTIME

fun foo() {
    val a = arrayListOf<Int>(1, 2, 3)
    a.<caret>addAll(1, arrayListOf(4))
}
