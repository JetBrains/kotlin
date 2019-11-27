// IGNORE_BACKEND_FIR: JVM_IR
// KJS_WITH_FULL_RUNTIME
// IGNORE_BACKEND: NATIVE
class MySet : HashSet<Int>() {
    override fun remove(element: Int): Boolean {
        return super.remove(element)
    }
}

fun box(): String {
    val a = MySet()
    a.add(1)
    a.add(2)
    a.add(3)

    if (!a.remove(1)) return "fail 1"
    if (a.remove(1)) return "fail 2"
    if (a.contains(1)) return "fail 3"

    return "OK"
}
