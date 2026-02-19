// ISSUE: KT-67517
// LANGUAGE: +AvoidWrongOptimizationOfTypeOperatorsOnValueClasses

inline class X(val x: String)
inline class Y(val x: Int)

fun box(): String = when {
    (X("") as Any) is Comparable<*> -> "1"
    (Y(2) as Any) is Comparable<*> -> "2"
    (X("") as Any) is Number -> "3"
    (Y(2) as Any) is Number -> "4"
    else -> "OK"
}
