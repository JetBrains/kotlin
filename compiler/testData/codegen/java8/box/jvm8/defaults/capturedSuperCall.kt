// !API_VERSION: 1.3
// !ENABLE_JVM_DEFAULT
// JVM_TARGET: 1.8
// WITH_RUNTIME

interface IBase {
    @JvmDefault
    fun bar() = "OK"
}

open class Base {
    fun foo() = "OK"
}

class C : Base(), IBase {
    val lambda1 = {
        super.foo()
    }

    val lambda2 = {
        super.bar()
    }
}

fun box(): String {
    if (C().lambda1() != "OK") return "fail 1"

    return C().lambda2()
}
