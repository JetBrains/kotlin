// IGNORE_FIR_DIAGNOSTICS
// K2 status: KT-66529 K2: MANY_IMPL_MEMBER_NOT_IMPLEMENTED on inheriting Java member with generic type and Kotlin member with primitive type

// FILE: A.java

public class A<T> {
    public String foo(T x) {
        return "A";
    }
}

// FILE: main.kt

interface B<T> {
    fun foo(x: T) = "B"
}

open class C : A<Int>()

interface D : B<Int>

<!CONFLICTING_INHERITED_JVM_DECLARATIONS!><!MANY_IMPL_MEMBER_NOT_IMPLEMENTED!>class E<!> : C(), D<!>

fun test() {
    E().foo(42)
}
