// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 1 java/lang/invoke/LambdaMetafactory

fun interface Cmp<T> {
    fun compare(a: T, b: T): Int
}

fun cmp(c: Cmp<*>) = c

fun box(): String {
    val c = cmp { _, _ -> 0 } as Cmp<Any>
    if (c.compare(1, 2) != 0)
        return "Failed"
    return "OK"
}
