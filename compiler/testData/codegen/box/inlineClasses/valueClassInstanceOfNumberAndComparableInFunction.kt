// ISSUE: KT-67518
// LANGUAGE: +AvoidWrongOptimizationOfTypeOperatorsOnValueClasses
// IGNORE_NATIVE: compatibilityTestMode=BACKWARD
// ^^^ Compiler v2.1.10 does not know this language feature

inline class X(val x: String)

fun box(): String = if (check(X("")) || checkInline(X(""))) "Fail" else "OK"

fun check(a: Any): Boolean = a is Comparable<*>

inline fun checkInline(a: Any): Boolean = a is Comparable<*>
