// IGNORE_BACKEND_FIR: JVM_IR
// KJS_WITH_FULL_RUNTIME
class ArrayWrapper<T>() {
    val contents = ArrayList<T>()

    fun add(item: T) {
        contents.add(item)
    }

    operator fun plusAssign(rhs: ArrayWrapper<T>) {
        contents.addAll(rhs.contents)
    }

    operator fun get(index: Int): T {
        return contents.get(index)!!
    }
}

fun box(): String {
    var v1 = ArrayWrapper<String>()
    val v2 = ArrayWrapper<String>()
    v1.add("foo")
    v2.add("bar")
    v1 += v2
    return if (v1.contents.size == 2) "OK" else "fail"
}
