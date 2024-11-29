// ISSUE: KT-67518
// LANGUAGE: +AvoidWrongOptimizationOfTypeOperatorsOnValueClasses

inline class X(val x: String)

fun box(): String = if (check(X("")) || checkInline(X(""))) "Fail" else "OK"

fun check(a: Any): Boolean = a is Comparable<*>

inline fun checkInline(a: Any): Boolean = a is Comparable<*>
