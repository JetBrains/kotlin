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
    JavaUsage.<!INAPPLICABLE_CANDIDATE!>foo<!> {
            x ->
        x <!UNRESOLVED_REFERENCE!>><!> 1
    }

    JavaUsage.<!INAPPLICABLE_CANDIDATE!>foo<!>({ <!UNRESOLVED_REFERENCE!>it<!> <!UNRESOLVED_REFERENCE!>><!> 1 })

    val x = { x: Int -> x > 1 }

    JavaUsage.<!INAPPLICABLE_CANDIDATE!>foo<!>(x)
}
