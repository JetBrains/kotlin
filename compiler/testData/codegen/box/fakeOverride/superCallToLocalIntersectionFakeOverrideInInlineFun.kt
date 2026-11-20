// IGNORE_BACKEND: NATIVE, JS_IR, JS_IR_ES6, WASM_JS, WASM_WASI
// IGNORE_IR_DESERIALIZATION_TEST: NATIVE, JS_IR
// ^^^ Muted because AvoidLocalFOsInInlineFunctionsLowering is not able to correctly rewrite IR calls to local FOs with local-super class
//     as IrCall.superQualifierSymbol. See KT-91456.

// MODULE: lib
// FILE: lib.kt

interface I {
    fun foo(): String
}

interface IP {
    val p: String
}

open class A {
    open fun foo(): String = "foo"
    open val p: String get() = "p"
}

inline fun test(): String {
    val a = object {
        fun foo(): String {
            open class InterfaceFirst : I, IP, A()
            open class ClassFirst : A(), I, IP

            class OverridesInterfaceFirst : InterfaceFirst() {
                override fun foo(): String = super.foo()
                override val p: String get() = super.p
            }

            class OverridesClassFirst : ClassFirst() {
                override fun foo(): String = super.foo()
                override val p: String get() = super.p
            }

            val anonymousInterfaceFirst = object : InterfaceFirst() {
                override fun foo(): String = super.foo()
                override val p: String get() = super.p
            }

            if (OverridesInterfaceFirst().foo() != "foo") return "Fail 1"
            if (OverridesInterfaceFirst().p != "p") return "Fail 2"
            if (OverridesClassFirst().foo() != "foo") return "Fail 3"
            if (OverridesClassFirst().p != "p") return "Fail 4"
            if (anonymousInterfaceFirst.foo() != "foo") return "Fail 5"
            if (anonymousInterfaceFirst.p != "p") return "Fail 6"

            return "OK"
        }
    }

    return a.foo()
}

// MODULE: main(lib)
// FILE: main.kt
fun box() = test()
