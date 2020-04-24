// IGNORE_BACKEND_FIR: JVM_IR
// KJS_WITH_FULL_RUNTIME
// IGNORE_BACKEND: NATIVE

class IntArrayList(): ArrayList<Int>() {
    override fun get(index: Int): Int = super.get(index)
}

fun box(): String {
    val a = IntArrayList()
    a.add(1)
    a[0]++
    return if (a[0] == 2) "OK" else "fail"
}
