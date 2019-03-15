// INTENTION_TEXT: "Convert to 'trimIndent'"
// WITH_RUNTIME
fun test() {
    val x =
            """
                |a
                |b
            """.<caret>trimMargin()
}