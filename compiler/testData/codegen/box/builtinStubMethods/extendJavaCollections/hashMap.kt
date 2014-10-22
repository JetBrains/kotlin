import java.util.HashMap

class A : HashMap<String, Double>()

fun box(): String {
    val a = A()
    val b = A()

    a.put("", 0.0)
    a.remove("")

    a.putAll(b)
    a.clear()

    a.keySet()
    a.values()
    a.entrySet()

    return "OK"
}
