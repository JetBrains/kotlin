// IS_APPLICABLE: false
// WITH_RUNTIME
fun test() {
    val x =
            """
                \a
                \b
            """.<caret>trimMargin("\\")
}