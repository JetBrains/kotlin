// ISSUE: KT-67518
// LANGUAGE: -AvoidWrongOptimizationOfTypeOperatorsOnValueClasses
// TARGET_BACKEND: JVM

inline class X(val x: String)

fun box(): String = if (check(X("")) || !checkInline(X(""))) "Fail" else "OK" // wrong behaviour

fun check(a: Any): Boolean = a is Comparable<*>

inline fun checkInline(a: Any): Boolean = a is Comparable<*>
