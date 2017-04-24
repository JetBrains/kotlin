// !CHECK_TYPE
// FILE: A.java
public class A {
    public String foo() {}
    public CharSequence foo() {}

    public static String bar() {}
    public static CharSequence bar() {}
}

// FILE: main.kt

fun foo(a: A) {
    a.foo() checkType { _<String>() }
    A.bar() checkType { _<String>() }
}
