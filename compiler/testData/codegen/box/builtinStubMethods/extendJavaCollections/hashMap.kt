// KJS_WITH_FULL_RUNTIME
// IGNORE_BACKEND: NATIVE
// IGNORE_BACKEND: JVM_IR
class A : HashMap<String, Double>()

fun box(): String {
    val a = A()
    val b = A()

    a.put("", 0.0)
    a.remove("")

    a.putAll(b)
    a.clear()

    a.keys
    a.values
    a.entries

    return "OK"
}
