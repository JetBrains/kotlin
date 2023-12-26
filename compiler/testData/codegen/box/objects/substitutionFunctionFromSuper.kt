// KT-44054
// JVM_ABI_K1_K2_DIFF: KT-63655
enum class Enum {
    Entry1,
    Entry2
}

class Outer {
    fun fooCaller(): Enum = obj.foo()

    private abstract inner class Inner<T>(val default: T) {
        fun foo(): T {
            return default
        }
    }

    private val obj = object : Inner<Enum>(Enum.Entry1) {
        fun bar(): Enum {
            return default
        }
    }
}

fun box(): String {
    val o = Outer()
    if (o.fooCaller() != Enum.Entry1) return "Fail"
    return "OK"
}
