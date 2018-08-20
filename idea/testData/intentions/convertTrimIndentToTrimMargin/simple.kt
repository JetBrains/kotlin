// INTENTION_TEXT: "Convert to 'trimMargin'"
// WITH_RUNTIME
fun test() {
    val x =
           """
                a

                b
            """.<caret>trimIndent()
}