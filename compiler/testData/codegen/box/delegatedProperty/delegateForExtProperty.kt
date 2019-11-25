// IGNORE_BACKEND_FIR: JVM_IR
import kotlin.reflect.KProperty

class Delegate {
  operator fun getValue(t: A, p: KProperty<*>): Int = 1
}

val A.prop: Int by Delegate()

class A {
}

fun box(): String {
  return if(A().prop == 1) "OK" else "fail"
}
