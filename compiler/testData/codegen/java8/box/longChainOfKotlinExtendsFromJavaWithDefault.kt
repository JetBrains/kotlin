// FILE: Base.java

public interface Base {
    default String foo() {
        return "OK";
    }
}

// FILE: derived.kt

interface K1 : Base

interface K2 : K1

interface K3 : K2

class C : K3 {
    @kotlin.Suppress("DEFAULT_METHOD_CALL_FROM_JAVA6_TARGET")
    override fun foo() = super.foo()
}

fun box(): String = C().foo()
