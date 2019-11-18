// IGNORE_BACKEND_FIR: JVM_IR
fun stringConcat(n : Int) : String? {
  var string : String? = ""
  for (i in 0..(n - 1))
    string += "LOL "
  return string
}

fun box() = if(stringConcat(3) == "LOL LOL LOL ") "OK" else "fail"
