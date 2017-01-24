// JVM_TARGET: 1.8
// KOTLIN_CONFIGURATION_FLAGS: +JVM.JVM8_TARGET_WITH_DEFAULTS

interface IBase {
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
