// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// FILE: IBase.java

interface IBase {
    default String bar() {
        return "OK";
    }
}

// FILE: Kotlin.kt
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
