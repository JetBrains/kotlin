// !WITH_NEW_INFERENCE
// !CHECK_TYPE
// !DIAGNOSTICS: -UNUSED_PARAMETER
// FILE: A.java

public class A {
    public static Out<? super CharSequence> foo() { return null; }
    public static In<? extends CharSequence> bar() { return null; }
}

// FILE: main.kt

class Out<out E> {
    fun x(): E = null!!
}

class In<in F> {
    fun y(f: F) {}
}

fun test() {
    A.foo().x() checkType { _<Any?>() }
    A.bar().<!MEMBER_PROJECTED_OUT!>y<!>(null)
}
