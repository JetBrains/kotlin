// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// FILE: box.kt

fun test(f: (String) -> Unit) {
    A.s(f)
}

fun box(): String {
    var result = "Fail"
    test { result = it }
    return result
}

// FILE: A.java

public interface A<T> {
    void f(T t);

    A<Object> N = new A<Object>() {
        @Override
        public void f(final Object object) {
        }
    };

    static void s(A<String> a) {
        a.f("OK");
    }
}
