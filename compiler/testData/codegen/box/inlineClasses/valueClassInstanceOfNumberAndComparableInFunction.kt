// ISSUE: KT-67518
// LANGUAGE: +AvoidWrongOptimizationOfTypeOperatorsOnValueClasses
// IGNORE_NATIVE: compatibilityTestMode=BACKWARD_1_9
// IGNORE_NATIVE: compatibilityTestMode=BACKWARD_2_0
// IGNORE_NATIVE: compatibilityTestMode=BACKWARD_2_1
// ^^^ Compiler v2.1.0 does not know this language feature

inline class X(val x: String)

fun box(): String = if (check(X("")) || checkInline(X(""))) "Fail" else "OK"

fun check(a: Any): Boolean = a is Comparable<*>

inline fun checkInline(a: Any): Boolean = a is Comparable<*>
