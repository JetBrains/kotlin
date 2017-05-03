class A {
  val a = """<caret>blah blah""".trimMargin("#")
}
//-----
class A {
  val a = """
      #<caret>blah blah""".trimMargin("#")
}