// IGNORE_BACKEND: JS_IR
// KT-6042 java.lang.UnsupportedOperationException with ArrayList
// IGNORE_BACKEND: NATIVE
class A : ArrayList<String>()

fun box(): String {
    val a = A()
    val b = A()

    a.addAll(b)
    a.addAll(0, b)
    a.removeAll(b)
    a.retainAll(b)
    a.clear()

    a.add("")
    a.set(0, "")
    a.add(0, "")
    a.removeAt(0)
    a.remove("")

    return "OK"
}
