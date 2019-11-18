// IGNORE_BACKEND_FIR: JVM_IR
interface AL {
    fun get(index: Int) : Any? = null
}

interface ALE<T> : AL {
    fun getOrNull(index: Int, value: T) : T {
        val r = get(index) as? T
        return r ?: value
    }
}

open class SmartArrayList() : ALE<String> {
}

class SmartArrayList2() : SmartArrayList(), AL {
}

fun box() : String {
  val c = SmartArrayList2()
  return if("239" == c.getOrNull(0, "239")) "OK" else "fail"
}
