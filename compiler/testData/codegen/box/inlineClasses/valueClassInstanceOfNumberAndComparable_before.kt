// ISSUE: KT-67517
// LANGUAGE: -AvoidWrongOptimizationOfTypeOperatorsOnValueClasses
// TARGET_BACKEND: JVM_IR

inline class X(val x: String)
inline class Y(val x: Int)

fun box(): String = when {
    (X("") as Any) !is Comparable<*> -> "1" // wrong behaviour
    (Y(2) as Any) !is Comparable<*> -> "2" // wrong behaviour
    (X("") as Any) is Number -> "3"
    (Y(2) as Any) !is Number -> "4" // wrong behaviour
    else -> "OK"
}
