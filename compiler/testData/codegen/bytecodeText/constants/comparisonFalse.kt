// TARGET_BACKEND: JVM_IR

fun foo(): Array<Boolean> {
    return arrayOf(
        0.0 / 0 == 0.0 / 0,
        0.0F > -0.0F,
        0.0.equals(-0.0),
        (0.0 / 0.0).equals(1.0 / 0.0)
    )
}

// Disabled: current backend doesn't fold them.
// 6 INVOKESTATIC
// 4 INVOKESTATIC java/lang/Boolean.valueOf
// 2 ICONST_1
// 6 ICONST_0
// 0 IFEQ
// 1 IFNE
// 0 IFGE
// 0 IFGT
// 0 IFLE
// 0 IFLT
