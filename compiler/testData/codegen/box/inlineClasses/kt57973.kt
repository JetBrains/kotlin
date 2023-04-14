// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// CHECK_BYTECODE_LISTING
// IGNORE_BACKEND: JVM

interface IIC {
    fun f(
        i1: Long = 1L,
        i2: Long = 2L,
        i3: Long = 3L,
        i4: Long = 4L,
        i5: Long = 5L,
        i6: Long = 6L,
        i7: Long = 7L,
        i8: Long = 8L,
        i9: Long = 9L,
        i10: Long = 10L,
        i11: Long = 11L,
        i12: Long = 12L,
        i13: Long = 13L,
        i14: Long = 14L,
        i15: Long = 15L,
        i16: Long = 16L,
        i17: Long = 17L,
        i18: Long = 18L,
        i19: Long = 19L,
        i20: Long = 20L,
        i21: Long = 21L,
        i22: Long = 22L,
        i23: Long = 23L,
        i24: Long = 24L,
        i25: Long = 25L,
        i26: Long = 26L,
        i27: Long = 27L,
        i28: Long = 28L,
        i29: Long = 29L,
        i30: Long = 30L,
        i31: Long = 31L,
        i32: Long = 32L,
        i33: Long = 33L,
    ): Long
    
    fun g(
        i1: Long = 1L,
        i2: Long = 2L,
        i3: Long = 3L,
        i4: Long = 4L,
        i5: Long = 5L,
        i6: Long = 6L,
        i7: Long = 7L,
        i8: Long = 8L,
        i9: Long = 9L,
        i10: Long = 10L,
        i11: Long = 11L,
        i12: Long = 12L,
        i13: Long = 13L,
        i14: Long = 14L,
        i15: Long = 15L,
        i16: Long = 16L,
        i17: Long = 17L,
        i18: Long = 18L,
        i19: Long = 19L,
        i20: Long = 20L,
        i21: Long = 21L,
        i22: Long = 22L,
        i23: Long = 23L,
        i24: Long = 24L,
        i25: Long = 25L,
        i26: Long = 26L,
        i27: Long = 27L,
        i28: Long = 28L,
        i29: Long = 29L,
        i30: Long = 30L,
        i31: Long = 31L,
        i32: Long = 32L,
    ): Long

}

inline class IC(val x: Long) : IIC {
    override fun f(
        i1: Long,
        i2: Long,
        i3: Long,
        i4: Long,
        i5: Long,
        i6: Long,
        i7: Long,
        i8: Long,
        i9: Long,
        i10: Long,
        i11: Long,
        i12: Long,
        i13: Long,
        i14: Long,
        i15: Long,
        i16: Long,
        i17: Long,
        i18: Long,
        i19: Long,
        i20: Long,
        i21: Long,
        i22: Long,
        i23: Long,
        i24: Long,
        i25: Long,
        i26: Long,
        i27: Long,
        i28: Long,
        i29: Long,
        i30: Long,
        i31: Long,
        i32: Long,
        i33: Long,
    ) = i1 + i2 + i3 + i4 + i5 + i6 + i7 + i8 + i9 + i10 + i11 + i12 + i13 + i14 + i15 + i16 +
            i17 + i18 + i19 + i20 + i21 + i22 + i23 + i24 + i25 + i26 + i27 + i28 + i29 + i30 + i31 + i32 + i33
    override fun g(
        i1: Long,
        i2: Long,
        i3: Long,
        i4: Long,
        i5: Long,
        i6: Long,
        i7: Long,
        i8: Long,
        i9: Long,
        i10: Long,
        i11: Long,
        i12: Long,
        i13: Long,
        i14: Long,
        i15: Long,
        i16: Long,
        i17: Long,
        i18: Long,
        i19: Long,
        i20: Long,
        i21: Long,
        i22: Long,
        i23: Long,
        i24: Long,
        i25: Long,
        i26: Long,
        i27: Long,
        i28: Long,
        i29: Long,
        i30: Long,
        i31: Long,
        i32: Long,
    ) = i1 + i2 + i3 + i4 + i5 + i6 + i7 + i8 + i9 + i10 + i11 + i12 + i13 + i14 + i15 + i16 +
            i17 + i18 + i19 + i20 + i21 + i22 + i23 + i24 + i25 + i26 + i27 + i28 + i29 + i30 + i31 + i32
}

fun box(): String {
    require(IC(2).f() == (1..33).sum().toLong()) {
        "${IC(2).f()} != ${(1..33).sum()}"
    }
    require(IC(2).g() == (1..32).sum().toLong()) {
        "${IC(2).g()} != ${(1..32).sum()}"
    }
    return "OK"
}
