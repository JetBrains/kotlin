// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

// WITH_RUNTIME

fun box() : String {
  val i = 1
  return if(i.javaClass.getSimpleName() == "int") "OK" else "fail"
}
