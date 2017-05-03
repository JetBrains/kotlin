fun some() {
    val b = """<caret>
        """.trimMargin()
}
//-----
fun some() {
    val b = """
        |<caret>
        """.trimMargin()
}
