fun box() : String {
  val s = """ foo \n bar """
  if (s != " foo \\n bar ") return "Fail: '$s'"

  return "OK"
}
