val a =
  """
     |  blah blah blah {<caret>}
  """.trimMargin()
//-----
val a =
  """
     |  blah blah blah {
     |  <caret>
     |  }
  """.trimMargin()