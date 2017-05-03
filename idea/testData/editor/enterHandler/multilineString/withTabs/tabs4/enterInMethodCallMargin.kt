val a = """blah blah<caret>""".replace("\r", "")
//-----
val a = """blah blah
	|<caret>
""".trimMargin().replace("\r", "")