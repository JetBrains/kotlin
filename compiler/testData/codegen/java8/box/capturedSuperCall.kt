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

@kotlin.Suppress("DEFAULT_METHOD_CALL_FROM_JAVA6_TARGET")
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
