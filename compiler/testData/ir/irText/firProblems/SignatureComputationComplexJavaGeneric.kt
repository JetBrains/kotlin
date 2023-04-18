// TARGET_BACKEND: JVM_IR

// FILE: J.java

import org.jetbrains.annotations.Nullable;

public class J<T extends @Nullable Object> {
    public class A {
        public void output(T x) {}
    }
    public class B extends A {}
}

// FILE: test.kt

class Test<T>(val x: T) : J<T>() {
    fun test(b: B) { b.output(x) }
}
