// IGNORE_BACKEND: JS_IR
// TODO: Enable for JS when it supports Java class library.
// IGNORE_BACKEND: JS, NATIVE
class List<T>(val head: T, val tail: List<T>? = null)

fun <T> List<T>.mapHead(f: (T)-> T): List<T> = List<T>(f(head), null)

fun box() : String {
  val a: Int = List<Int>(1).mapHead{it * 2}.head
  System.out?.println(a)
  return "OK"
}
