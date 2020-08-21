// TARGET_BACKEND: JVM
// FILE: A.java
public class A<T> {
    public interface I<T> {
        String apply(T x);
    }

    public String call(I<T> block) { return block.apply(null); }
}

// FILE: test.kt
fun f(x: Any): String {
    if (x is A<*>) {
        return x.call { y: Any? -> "OK" }
    }
    return "Fail"
}

fun box(): String {
    return f(A<Int>())
}
