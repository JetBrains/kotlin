import java.util.AbstractMap
import java.util.Collections

class A : AbstractMap<Int, String>() {
    override fun entrySet(): Set<Map.Entry<Int, String>> = Collections.emptySet()
}

fun box(): String {
    val a = A()
    val b = A()

    a.remove(0)

    a.putAll(b)
    a.clear()

    a.keySet()
    a.values()
    a.entrySet()

    return "OK"
}
