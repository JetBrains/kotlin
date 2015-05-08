// "Create extension property 'foo'" "true"
// ERROR: Property must be initialized

fun test() {
    val a: Int = Unit.<caret>foo
}
