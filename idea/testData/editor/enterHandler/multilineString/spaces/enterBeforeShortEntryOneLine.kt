val className = 1
val test = """<caret>$className"""
//-----
val className = 1
val test = """
    <caret>$className""".trimIndent()
