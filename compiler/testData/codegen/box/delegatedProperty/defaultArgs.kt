import kotlin.reflect.KProperty

class Delegate {
    fun getValue(t: Any?, p: KProperty<*>, s: String = ""): Int = 1
}

val prop: Int by Delegate()

fun box(): String {
  return if(prop == 1) "OK" else "fail"
}
