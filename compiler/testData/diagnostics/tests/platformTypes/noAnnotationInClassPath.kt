// FILE: A.java

// It's supposed that there is no JSR-305 annotation in classpath
public interface A<T> {
    public boolean foo(@javax.annotation.Nullable T y) {}
}

// FILE: B.java

public class B {
    public static void bar(A<String> y) {}
}

// FILE: main.kt
fun test() {
    B.bar() { it<!UNSAFE_CALL!>.<!>hashCode() > 0 }
}
