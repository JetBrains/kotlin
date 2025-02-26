// LANGUAGE: +MultiPlatformProjects
// IGNORE_NATIVE: compatibilityTestMode=BACKWARD_2_0
// ^^^ Compiler v2.0.0: IrSimpleFunctionSymbolImpl is unbound. Signature: null

// MODULE: common
// FILE: common.kt
expect interface Base {
    fun foo(): String
}

class DelegatedImpl(val foo: Base) : Base by foo

// MODULE: platform()()(common)
// FILE: platform.kt
actual interface Base {
    actual fun foo(): String
    fun bar(): String
}

class Impl : Base {
    override fun foo(): String = "O"
    override fun bar(): String = "K"
}

fun box(): String {
    val x = DelegatedImpl(Impl())
    return x.foo() + x.bar()
}