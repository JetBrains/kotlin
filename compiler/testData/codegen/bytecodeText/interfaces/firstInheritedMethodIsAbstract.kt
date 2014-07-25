interface A {
    fun foo(): String
}

interface B {
    fun foo(): String = "OK"
}

interface C : A, B

// There's no 'foo' in A$$TImpl, proguard and other tools may fail if we generate calls to it
// 0 INVOKESTATIC A\$\$TImpl.foo
// 1 INVOKESTATIC B\$\$TImpl.foo
