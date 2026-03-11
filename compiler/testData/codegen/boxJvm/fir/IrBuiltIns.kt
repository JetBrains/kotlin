// TARGET_BACKEND: JVM
// MODULE: lib
// FILE: A.kt

class IrBuiltIns {
    object OperatorNames {
        const val LESS = "OK"
    }
}

// MODULE: main(lib)
// FILE: B.kt

fun foo(s: String) = s

fun box(): String {
    return foo(IrBuiltIns.OperatorNames.LESS)
}
