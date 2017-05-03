val a = """blah blah blah<caret>""".replace(" ", "")?.replace("b", "p").trimMargin().length
//-----
val a = """blah blah blah
    |<caret>
""".replace(" ", "")?.replace("b", "p").trimMargin().length