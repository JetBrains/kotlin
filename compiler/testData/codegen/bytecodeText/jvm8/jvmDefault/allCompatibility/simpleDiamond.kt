// JVM_DEFAULT_MODE: all-compatibility
// JVM_TARGET: 1.8

interface A {

    fun foo() = "FAIL"
}

interface Left : A { }
interface Right : A {

    override fun foo() = "OK"
}

interface C : Left, Right {}

fun box(): String {
    val x = object : C {}
    return x.foo()
}

// 0 INVOKESTATIC .*\$DefaultImpls\.foo
