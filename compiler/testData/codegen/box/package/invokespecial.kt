// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

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
