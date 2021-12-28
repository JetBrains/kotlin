// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class Z<T: Long>(val value: T)

fun f(g: (
    z: Z<Long>,
    p01: Long, p02: Long, p03: Long, p04: Long, p05: Long, p06: Long, p07: Long, p08: Long, p09: Long, p10: Long,
    p11: Long, p12: Long, p13: Long, p14: Long, p15: Long, p16: Long, p17: Long, p18: Long, p19: Long, p20: Long,
    p21: Long, p22: Long
) -> Unit) {
    g(Z(42L), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
}

fun box(): String {
    var result = ""
    f { z, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _ ->
        result = if (z.value == 42L) "OK" else "FAIL"
    }
    return result
}
