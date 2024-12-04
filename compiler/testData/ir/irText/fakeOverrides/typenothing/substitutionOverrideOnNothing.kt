// SKIP_KT_DUMP
// TARGET_BACKEND: JVM

// FILE: Java1.java

public class Java1<T> {
    public T a;

    public void foo(T t) {};

    public T bar() {
        return a;
    };
}


// FILE: 1.kt
class A : Java1<Nothing>()

class B : Java1<Nothing?>()

fun test(a: A, b: B){
    val k: Nothing = a.a
    val k2: Nothing = a.bar()
    val k3: Unit = a.foo(k)

    val k4: Nothing? = b.a
    val k5: Nothing? = b.bar()
    val k6: Unit = b.foo(k)
}