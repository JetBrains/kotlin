// "Create extension property 'foo'" "true"
// ERROR: Property must be initialized
// WITH_RUNTIME

fun test() {
    val a: Int = Unit.<caret>foo
}
