// JVM_DEFAULT_MODE: enable

interface A {
    fun foo(): String
}

interface B {
    fun foo(): String = "OK"
}

interface C : A, B {
    override fun foo(): String = super<B>.foo()
}

// In the 'enable' mode, methods from DefaultImpls should never be called.
// 0 INVOKESTATIC A\$DefaultImpls.foo
// 0 INVOKESTATIC B\$DefaultImpls.foo

// There are two calls to B.foo: one is a supercall from C.foo, another is a non-virtual call from B.access$getFoo$jd (which is called from DefaultImpls).
// 2 INVOKESPECIAL B.foo
