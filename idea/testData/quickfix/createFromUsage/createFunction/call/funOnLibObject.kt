// "Create extension function 'foo'" "true"
// WITH_RUNTIME

fun test() {
    val a: Int = Unit.<caret>foo(2)
}