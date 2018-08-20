// IS_APPLICABLE: false
// WITH_RUNTIME
fun test(marginPrefix: String) {
    val x =
            """
                |a
                |b
            """.<caret>trimMargin(marginPrefix)
}