// FILE: inlineClasses.kt
inline class A(val x: Int)
inline class B(val x: String)
inline class C(val x: Any?)

// FILE: a.kt

// False
fun isNullVacuousLeftA(s: A) = s == null
fun isNullVacuousRightA(s: A) = null == s
// IFNONNULL
fun isNullLeftA(s: A?) = s == null
fun isNullRightA(s: A?) = null == s
// equals-impl0
fun isEqualSameA(s: A, t: A) = s == t
// equals-impl
fun isEqualAnyLeftA(s: A, t: Any?) = s == t
// boxes
// fun isEqualAnyRightA(s: Any?, t: A) = s == t
// Intrinsics.areEqual
fun isEqualSameNullableA(s: A?, t: A?) = s == t
fun isEqualAnyNullableLeftA(s: A?, t: Any?) = s == t
fun isEqualAnyNullableRightA(s: Any?, t: A?) = s == t
// unbox, equals-impl0
fun isEqualLeftNullableRightUnboxedA(s: A?, t: A) = s == t
// equals-impl
fun isEqualRightNullableLeftUnboxedA(s: A, t: A?) = s == t

// FILE: b.kt

// False
fun isNullVacuousLeftB(s: B) = s == null
fun isNullVacuousRightB(s: B) = null == s
// IFNONNULL
fun isNullLeftB(s: B?) = s == null
fun isNullRightB(s: B?) = null == s
// equals-impl0
fun isEqualSameB(s: B, t: B) = s == t
// equals-impl
fun isEqualAnyLeftB(s: B, t: Any?) = s == t
// boxes
// fun isEqualAnyRightB(s: Any?, t: B) = s == t
// equals-impl0
fun isEqualSameNullableB(s: B?, t: B?) = s == t
// equals-impl
fun isEqualAnyNullableLeftB(s: B?, t: Any?) = s == t
// boxes
// fun isEqualAnyNullableRightB(s: Any?, t: B?) = s == t
// equals-impl0
fun isEqualLeftNullableRightUnboxedB(s: B?, t: B) = s == t
fun isEqualRightNullableLeftUnboxedB(s: B, t: B?) = s == t

// FILE: c.kt

// False
fun isNullVacuousLeftC(s: C) = s == null
fun isNullVacuousRightC(s: C) = null == s
// IFNONULL
fun isNullLeftC(s: C?) = s == null
fun isNullRightC(s: C?) = null == s
// equals-impl0
fun isEqualSameC(s: C, t: C) = s == t
// equals-impl
fun isEqualAnyLeftC(s: C, t: Any?) = s == t
// boxes
// fun isEqualAnyRightC(s: Any?, t: C) = s == t
// Intrinsics.areEqual
fun isEqualSameNullableC(s: C?, t: C?) = s == t
fun isEqualAnyNullableLeftC(s: C?, t: Any?) = s == t
fun isEqualAnyNullableRightC(s: Any?, t: C?) = s == t
// unbox, equals-impl0
fun isEqualLeftNullableRightUnboxedC(s: C?, t: C) = s == t
// equals-impl
fun isEqualRightNullableLeftUnboxedC(s: C, t: C?) = s == t

// @AKt.class:
// 0 INVOKESTATIC A.box-impl
// 1 INVOKEVIRTUAL A.unbox-impl
// 2 INVOKESTATIC A.equals-impl \(
// 2 INVOKESTATIC A.equals-impl0
// 3 INVOKESTATIC kotlin/jvm/internal/Intrinsics.areEqual

// @BKt.class:
// 0 INVOKESTATIC B.box-impl
// 0 INVOKEVIRTUAL B.unbox-impl
// 2 INVOKESTATIC B.equals-impl \(
// 4 INVOKESTATIC B.equals-impl0
// 0 INVOKESTATIC kotlin/jvm/internal/Intrinsics.areEqual

// @CKt.class:
// 0 INVOKESTATIC C.box-impl
// 1 INVOKEVIRTUAL C.unbox-impl
// 2 INVOKESTATIC C.equals-impl \(
// 2 INVOKESTATIC C.equals-impl0
// 3 INVOKESTATIC kotlin/jvm/internal/Intrinsics.areEqual