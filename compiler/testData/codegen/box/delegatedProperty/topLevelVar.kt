class Delegate {
    var inner = 1
    fun get(t: Any?, p: String): Int = inner
    fun set(t: Any?, p: String, i: Int) { inner = i }
}

var prop: Int by Delegate()

fun box(): String {
  if(prop != 1) return "fail get"
  prop = 2
  if (prop != 2) return "fail set"
  return "OK"
}
