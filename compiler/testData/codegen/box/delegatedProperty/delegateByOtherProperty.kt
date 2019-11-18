// IGNORE_BACKEND_FIR: JVM_IR
import kotlin.reflect.KProperty

class Delegate {
    var inner = 1
    operator fun getValue(t: Any?, p: KProperty<*>): Int = inner
    operator fun setValue(t: Any?, p: KProperty<*>, i: Int) { inner = i }
}

class A {
    val p = Delegate()
    var prop: Int by p
}

fun box(): String {
  val c = A()
  if(c.prop != 1) return "fail get"
  c.prop = 2
  if (c.prop != 2) return "fail set"
  return "OK"
}
