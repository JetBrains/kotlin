// IGNORE_BACKEND: JVM_IR
// TODO KT-36651 Avoid boxing in safe call / elvis chains in JVM_IR

class A(val x : Int, val y : A?)

fun check(a : A?) : Int {
    return a?.y?.x ?: (a?.x ?: 3)
}

// 0 valueOf
// 0 Value\s\(\)
// 0 ACONST_NULL
