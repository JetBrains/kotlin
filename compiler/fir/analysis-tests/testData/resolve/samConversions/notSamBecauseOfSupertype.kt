// FILE: MyRunnable.java
public interface MyRunnable {
    void bar();
}

// FILE: DerivedRunnable.java
public interface DerivedRunnable extends MyRunnable {
    boolean foo(int x);
}

// FILE: JavaUsage.java

public class JavaUsage {
    public static void foo(DerivedRunnable x) {}
}
// FILE: main.kt

fun foo(m: MyRunnable) {}

fun main() {
    JavaUsage.foo {
            x ->
        <!ARGUMENT_TYPE_MISMATCH!><!ARGUMENT_TYPE_MISMATCH!>x<!> <!UNRESOLVED_REFERENCE!>><!> 1<!>
    }

    JavaUsage.foo({ <!ARGUMENT_TYPE_MISMATCH!><!UNRESOLVED_REFERENCE!>it<!> > 1<!> })

    val x = { x: Int -> x > 1 }

    JavaUsage.foo(<!ARGUMENT_TYPE_MISMATCH!>x<!>)
}
