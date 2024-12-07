// DO_NOT_CHECK_SYMBOL_RESTORE_K1

interface I1 {
    fun foo()
}

class I1Impl: I1 {
    override fun foo() {}
}

interface I2 {
    fun bar()
    fun baz()
}

class I2Impl: I2 {
    override fun bar() {}
    override fun baz() {}
}

open class A : I1 by I1Impl()

class B : I2 by I2Impl(), A() {
    override fun baz() {}
}

// class: B
