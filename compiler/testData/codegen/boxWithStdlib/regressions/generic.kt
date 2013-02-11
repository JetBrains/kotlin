import java.util.*

fun <T> ArrayList<T>.findAll(predicate:  (T) -> Boolean): ArrayList<T> {
  val result = ArrayList<T>()
  for(t in this) {
    if (predicate(t)) result.add(t)
  }
  return result
}


fun box(): String {
  val list: ArrayList<String> = ArrayList<String>()
  list.add("Prague")
  list.add("St.Petersburg")
  list.add("Moscow")
  list.add("Munich")

  val m: ArrayList<String> = list.findAll<String>({(name: String) -> name.startsWith("M")})
  return if (m.size() == 2) "OK" else "fail"
}
