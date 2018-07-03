// WITH_RUNTIME
fun test() {
    val x =
            """
                a
                    b
                        c
            """.<caret>trimIndent()
}