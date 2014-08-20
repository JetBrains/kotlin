// FILE: A.java
public class A {}

// FILE: X.java
public class X<T> {
    T foo() {return null;}
    void bar(T a) {}
}

// FILE: Y.java
public class Y extends X<A> {

}

// FILE: test.kt

fun main() {
    Y().foo()<!UNSAFE_CALL!>.<!>hashCode()
    Y().bar(null)
}
