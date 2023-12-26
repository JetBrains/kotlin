// KJS_WITH_FULL_RUNTIME
// JVM_ABI_K1_K2_DIFF: KT-63864

class ArrayWrapper<T>() {
    val contents = ArrayList<T>()

    fun add(item: T) {
       contents.add(item)
    }

    operator fun plus(b: ArrayWrapper<T>): ArrayWrapper<T> {
        val result = ArrayWrapper<T>()
        result.contents.addAll(contents)
        result.contents.addAll(b.contents)
        return result
    }
}

fun box(): String {
    val v1 = ArrayWrapper<String>()
    val v2 = ArrayWrapper<String>()
    v1.add("foo")
    v2.add("bar")
    val v3 = v1 + v2
    return if (v3.contents.size == 2) "OK" else "fail"
}
