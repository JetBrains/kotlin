val a =
  ("""
      |  blah blah
      |  blah blah
   """ + """<caret>""").trimMargin()

// TODO: Concatenation is not supported
//-----
val a =
  ("""
      |  blah blah
      |  blah blah
   """ + """
       <caret>
   """.trimIndent()).trimMargin()

// TODO: Concatenation is not supported
