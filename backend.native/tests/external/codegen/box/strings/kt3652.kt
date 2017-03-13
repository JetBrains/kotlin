fun box(): String {
 var a = 'a'

  if ("${a++}x" != "ax") return "fail1"

  if ("${a++}" != "b") return "fail2"

  return "OK"
}
