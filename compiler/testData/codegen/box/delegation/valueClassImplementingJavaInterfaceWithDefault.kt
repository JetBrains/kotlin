// TARGET_BACKEND: JVM_IR
// JVM_TARGET: 1.8
// ISSUE: KT-60876
// WITH_STDLIB

// FILE: A.java
public interface A {
    String foo(String value, String otherString);

    default String foo(String value ) {
        return foo(value, "K");
    }
}

// FILE: B.java
public class B implements A {
    public String foo(String value , String otherString) {
        return value + otherString;
    }
}

// FILE: main.kt
@JvmInline
value class C(val m: A) : A by m

fun box(): String {
    val myJava = C(B())
    return myJava.foo("O")
}
