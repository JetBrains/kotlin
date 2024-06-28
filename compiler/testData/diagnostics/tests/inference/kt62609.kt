// FIR_IDENTICAL
// ISSUE: KT-62609
// FILE: I.java
public interface I<T> {}

// FILE: X.java
public abstract class X<P> {}

// FILE: A.java
public final class A extends X<String> implements I<String> {
    public static final A INSTANCE = new A();
}

// FILE: B.java
public final class B extends X<Integer> implements I<Integer> {
    public static final B INSTANCE = new B();
}

// FILE: test.kt
fun test1() {
    controlFun(A.INSTANCE)
    controlFun(B.INSTANCE)
    controlFun2(A.INSTANCE)
    controlFun2(B.INSTANCE)

    val a = when {
        true -> A.INSTANCE
        else -> B.INSTANCE
    }

    controlFun(a)
    controlFun2(a)
}

fun test2() {
    controlFun(A())
    controlFun(B())
    controlFun2(A())
    controlFun2(B())

    val a = when {
        true -> A()
        else -> B()
    }

    controlFun(a)
    controlFun2(a)
}


fun <T> controlFun(c: I<T>) {}
fun <T> controlFun2(c: X<T>) {}
