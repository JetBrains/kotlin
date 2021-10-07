// MODULE: lib
// FILE: a.kt
class A(val x : Int, val y : A?)

// MODULE: main(lib)
// FILE: main.kt
fun check(a : A?) : Int {
    return a?.y?.x ?: (a?.x ?: 3)
}

// JVM_TEMPLATES:
// 0 ACONST_NULL
// 0 valueOf
// 0 Value\s\(\)

// JVM_IR_TEMPLATES:
// 1 valueOf
// 1 Value\s\(\)
// 2 ACONST_NULL
