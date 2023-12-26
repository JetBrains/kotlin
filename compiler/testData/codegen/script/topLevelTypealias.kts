// JVM_ABI_K1_K2_DIFF: KT-63960, KT-63963
class SimpleClass(val s: String) {
    fun foo() = s
}

typealias Test = SimpleClass

val rv = Test("OK").foo()

// expected: rv: OK
