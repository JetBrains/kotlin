// FIR_DUMP
// FILE: Function1.java
public interface Function1<A extends B, B extends A> {
    B handle(A a);
}

// FILE: Function2.java
public interface Function2<A extends B, B extends C, C extends A> {
    C handle(A a, B b);
}

// FILE: A.java
public class A {
    public static void foo(Function1<?, ?> f) {}
    public static void bar(Function2<?, ?, ?> f) {}
}

// FILE: main.kt
fun test_1() {
    A.foo { <!CANNOT_INFER_PARAMETER_TYPE!>a<!> ->
        null!!
    }
}

fun test_2() {
    A.foo <!CANNOT_INFER_PARAMETER_TYPE!>{
        null!!
    }<!>
}

fun test_3() {
    A.bar { <!CANNOT_INFER_PARAMETER_TYPE!>a<!>, <!CANNOT_INFER_PARAMETER_TYPE!>b<!> ->
        null!!
    }
}
