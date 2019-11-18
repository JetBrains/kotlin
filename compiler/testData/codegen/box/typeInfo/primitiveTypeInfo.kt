// IGNORE_BACKEND_FIR: JVM_IR
class Box<T>(t: T) {
    var value = t
}

fun isIntBox(box: Box<out Any?>): Boolean {
    return box is Box<*>;
}


fun box(): String {
  val box = Box<Int>(1)
  return if (isIntBox(box)) "OK" else "fail"
}
