// !JVM_DEFAULT_MODE: compatibility
// JVM_TARGET: 1.8

interface A {
    @JvmDefault
    fun foo() = "FAIL"
}

interface Left : A { }
interface Right : A {
    @JvmDefault
    override fun foo() = "OK"
}

interface C : Left, Right {}

fun box(): String {
    val x = object : C {}
    return x.foo()
}

// 0 INVOKESTATIC .*\$DefaultImpls\.foo
