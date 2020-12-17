// JVM_TARGET: 1.8

// FILE: A.java

public interface A {
    default String getMessage() {
        return "OK";
    }
}

// FILE: 1.kt

interface I : A

class B : A

open class C(a : A) : I, A by a

fun box(): String {
    val a = B()
    return C(a).message
}
