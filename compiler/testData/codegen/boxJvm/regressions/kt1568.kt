// TARGET_BACKEND: JVM

// WITH_STDLIB

fun box() : String {
  val i = 1
  return if(i.javaClass.getSimpleName() == "int") "OK" else "fail"
}
