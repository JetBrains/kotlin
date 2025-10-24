// JVM_ABI_K1_K2_DIFF: KT-63984, KT-76258

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
