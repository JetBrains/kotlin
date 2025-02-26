// LANGUAGE: +MultiPlatformProjects
// IGNORE_NATIVE: compatibilityTestMode=BACKWARD_2_0
// ^^^ Compiler v2.0.0: IrSimpleFunctionSymbolImpl is unbound. Signature: null

// MODULE: common
// FILE: common.kt

package test

expect fun interface Base {
    fun String.print(): String
}

expect fun String.foo(): String

class Derived(b: Base) : Base by b

// MODULE: platform()()(common)
// FILE: platform.kt

package test

actual fun interface Base {
    actual fun String.print(): String
}

actual fun String.foo(): String { return this }

fun box(): String {
    val a = Derived(Base(String::foo))
    with(a){
        return "OK".print()
    }
}