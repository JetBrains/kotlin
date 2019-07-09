// TARGET_BACKEND: JVM_IR

fun foo(): Array<Boolean> {
    return arrayOf(
        19 < 20.0,
        12 > 11,
        3.0F <= 4.0,
        4.0F >= 4,
        0.0 / 0 != 0.0 / 0,
        0.0 == -0.0,
        123 == 123,
        123L == 123L,
        0.0F == -0.0F,
        0.0.compareTo(-0.0) > 0,
        (0.0 / 0.0).compareTo(1.0 / 0.0) > 0
    )
}

// Disabled because the current backend doesn't fold them.
// 11 INVOKESTATIC
// 11 INVOKESTATIC java/lang/Boolean.valueOf
// 1 ICONST_0
// 12 ICONST_1
// 0 IFEQ
// 0 IFNE
// 0 IFGE
// 0 IFGT
// 0 IFLE
// 0 IFLT
