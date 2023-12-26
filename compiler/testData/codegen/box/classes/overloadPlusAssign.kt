// KJS_WITH_FULL_RUNTIME
// JVM_ABI_K1_K2_DIFF: KT-63864

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
