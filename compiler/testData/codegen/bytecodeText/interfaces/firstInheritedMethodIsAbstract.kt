interface A {
    fun foo(): String
}

interface B {
    fun foo(): String = "OK"
}

interface C : A, B {
    override fun foo(): String = super<B>.foo()
}

// There's no 'foo' in A$DefaultImpls, proguard and other tools may fail if we generate calls to it
// 0 INVOKESTATIC A\$DefaultImpls.foo
// 1 INVOKESTATIC B\$DefaultImpls.foo
