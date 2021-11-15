// WITH_STDLIB
inline class IC(val x: Int)

class C {
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("test")
    fun test() = IC(42)

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("differentName")
    fun test2() = IC(42)
}

fun call1() = C().test()

fun call2() = C().test2()

// 1 public final test\(\)I
// 1 INVOKEVIRTUAL C.test \(\)I
// 1 public final differentName\(\)I
// 1 INVOKEVIRTUAL C.differentName \(\)I
