fun some() {
    val b = """class Test {<caret>}"""
}
//-----
fun some() {
    val b = """class Test {
        |<caret>
        |}""".trimMargin()
}