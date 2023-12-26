// JVM_ABI_K1_K2_DIFF: KT-63960, KT-63968, KT-63967, KT-63963

val (_, b, _) = A()

class A {
    operator fun component1(): Int = throw RuntimeException()
    operator fun component2() = 2
    operator fun component3(): Int = throw RuntimeException()
}

// expected: b: 2