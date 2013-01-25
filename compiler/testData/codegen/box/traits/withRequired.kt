open class AL<T> {
    fun get(index: Int) : T? = null
}

trait ALE<T> : AL<T> {
    fun getOrValue(index: Int, value : T) : T = get(index) ?: value
}

class SmartArrayList() : ALE<String>, AL<String>() {
}

fun box() : String {
  val c = SmartArrayList()
  return if("239" == c.getOrValue(0, "239")) "OK" else "fail"
}
