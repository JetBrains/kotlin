class Delegate {
    fun get(t: Any?, p: String, s: String = ""): Int = 1
}

val prop: Int by Delegate()

fun box(): String {
  return if(prop == 1) "OK" else "fail"
}
