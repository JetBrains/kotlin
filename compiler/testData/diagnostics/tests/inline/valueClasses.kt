// LANGUAGE: +ValueClasses
// TARGET_BACKEND: JVM_IR
// WITH_STDLIB
// SKIP_TXT
// FIR_IDENTICAL

@JvmInline
value class DPoint(val x: Double, val y: Double)

inline fun f1() = DPoint(1.0, 2.0)
<!NOTHING_TO_INLINE!>inline<!> fun f2() = 2U
inline val p1 get() = DPoint(1.0, 2.0)
