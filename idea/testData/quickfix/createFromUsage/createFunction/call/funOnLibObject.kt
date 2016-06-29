// "Create extension function 'Unit.foo'" "true"
// WITH_RUNTIME

fun test() {
    val a: Int = Unit.<caret>foo(2)
}