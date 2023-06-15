class List<T>(val head: T, val tail: List<T>? = null)

fun <T> List<T>.mapHead(f: (T)-> T): List<T> = List<T>(f(head), null)

fun box() : String {
  val a: Int = List<Int>(1).mapHead{it * 2}.head
  return if (a == 2) "OK" else a.toString()
}
