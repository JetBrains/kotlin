fun some() {
    val b = """<caret>
        |hello
        """.trimMargin()
}
//-----
fun some() {
    val b = """
        |<caret>
        |hello
        """.trimMargin()
}
