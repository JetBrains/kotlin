// MODULE: lib
// FILE: a.kt
class A(val x : Int, val y : A?)

// MODULE: main(lib)
// FILE: main.kt
fun check(a : A?) : Int {
    return a?.y?.x ?: (a?.x ?: 3)
}

// 0 valueOf
// 0 Value\s\(\)

// JVM_TEMPLATES:
// 0 ACONST_NULL

// JVM_IR_TEMPLATES:
// 1 ACONST_NULL
