// JVM_ABI_K1_K2_DIFF: KT-63850, KT-63854

class A {
    private var foo = 1
        get() {
            return 1
        }

    fun foo() {
        foo = 5
        foo
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
    private var foo = 1
        set(i: Int) {
            field = i + 1
        }

    fun foo() {
        foo = 5
        foo
    }
}

fun box(): String {
    A().foo()
    B().foo()
    C().foo()
    D().foo()
    return "OK"
}

