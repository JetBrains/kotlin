fun some() {
    val b = """
        |helle<caret>|asdf
        """.trimMargin()
}
//-----
fun some() {
    val b = """
        |helle
        <caret>|asdf
        """.trimMargin()
}