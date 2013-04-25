class Delegate {
    fun get(t: Any?, vararg p: PropertyMetadata): Int = 1
}

val prop: Int by Delegate()

fun box(): String {
  return if(prop == 1) "OK" else "fail"
}
