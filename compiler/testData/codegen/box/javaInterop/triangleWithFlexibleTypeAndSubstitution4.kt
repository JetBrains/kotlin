// TARGET_BACKEND: JVM_IR

// IGNORE_BACKEND_K2: JVM_IR
// K2 status: KT-66529 K2: MANY_IMPL_MEMBER_NOT_IMPLEMENTED on inheriting Java member with generic type and Kotlin member with primitive type

// FILE: A.java

public class A<T> {
    public String foo(T x) {
        return "Fail";
    }
}

// FILE: main.kt

interface B<T> {
    fun foo(x: T) = "OK"
}

open class C : A<Int>()

interface D : B<Int>

class E : C(), D

fun box(): String {
    return E().foo(42)
}
