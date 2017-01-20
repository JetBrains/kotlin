// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_RUNTIME

fun box() : String {
  val i = 1
  return if(i.javaClass.getSimpleName() == "int") "OK" else "fail"
}
