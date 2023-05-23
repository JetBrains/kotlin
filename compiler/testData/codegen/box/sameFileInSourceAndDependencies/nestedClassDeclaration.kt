// DONT_TARGET_EXACT_BACKEND: NATIVE
// NATIVE error: error: compilation failed: IrClassPublicSymbolImpl for box.sameFileInSourceAndDependencies.nestedClassDeclaration/Host|null[0] is already bound: CLASS CLASS name:Host modality:FINAL visibility:public superTypes:[kotlin.Any]
// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// JS_IR error: IrClassPublicSymbolImpl for /Host|null[0] is already bound: CLASS CLASS name:Host modality:FINAL visibility:public superTypes:[kotlin.Any]
// IGNORE_BACKEND: WASM

// MODULE: lib
// FILE: 2.kt
class Host {
    abstract class B : A()

    abstract class A {
        private val value = "OK"
        fun f() = value
    }
}

// FILE: 3.kt
abstract class C : Host.B()

// MODULE: main(lib)
// FILE: 1.kt
class D : C()

fun box(): String = D().f()

// FILE: 2.kt
class Host {
    abstract class B : A()

    abstract class A {
        private val value = "OK"
        fun f() = value
    }
}
