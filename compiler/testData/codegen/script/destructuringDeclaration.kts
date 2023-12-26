// DUMP_IR
// JVM_ABI_K1_K2_DIFF: KT-63960,, KT-63967

val (abc, def) = A()

val rv = abc + def

class A {
    operator fun component1() = 123
    operator fun component2() = 2
}

// expected: rv: 125