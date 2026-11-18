// constructor: /JavaClass.init
// KT-91552
// FILE: A.java
public interface A {}

// FILE: B.java
public interface B {}

// FILE: JavaClass.java
public class JavaClass<E extends CharSequence> {
    public <T extends A> JavaClass(T x) {}
    public <T extends B> JavaClass(T x) {}
}

// FILE: main.kt
