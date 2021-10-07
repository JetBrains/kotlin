class A(val x : Int, val y : A?)

fun check(a : A?) : Int {
    return a?.y?.x ?: (a?.x ?: 3)
}

// JVM_TEMPLATES:
// 0 valueOf
// 0 Value\s\(\)
// 0 ACONST_NULL

// JVM_IR_TEMPLATES:
// 1 valueOf
// 1 Value\s\(\)
// 2 ACONST_NULL
