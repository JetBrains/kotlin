class Delegate {
    var inner = 1
    fun getValue(t: Any?, p: PropertyMetadata): Int = inner
    fun setValue(t: Any?, p: PropertyMetadata, i: Int) { inner = i }
}

var prop: Int by Delegate()

fun box(): String {
  if(prop != 1) return "fail get"
  prop = 2
  if (prop != 2) return "fail set"
  return "OK"
}
