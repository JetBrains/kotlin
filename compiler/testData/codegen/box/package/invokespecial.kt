// IGNORE_BACKEND_K1: WASM
// IGNORE_BACKEND_K1: JS_IR, JS_IR_ES6
// IGNORE_BACKEND_K2: JS_IR, JS_IR_ES6
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS
// JVM_ABI_K1_K2_DIFF: KT-63850, KT-63984

// KT-2202 Wrong instruction for invoke private setter

class A {
    private fun f1() { }
    fun foo() {
        f1()
    }
}

class B {
    private val foo = 1
        get

    fun foo() {
        foo
    }
}

class C {
    private var foo = 1
        get
        set

    fun foo() {
        foo = 2
        foo
    }
}

class D {
    var foo = 1
        private set

    fun foo() {
        foo = 2
    }
}

fun box(): String {
   A().foo()
   B().foo()
   C().foo()
   D().foo()
   return "OK"
}
