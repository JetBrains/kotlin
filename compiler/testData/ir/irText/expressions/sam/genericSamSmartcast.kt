// FILE: genericSamSmartcast.kt
fun f(x: Any): String {
    if (x is A<*>) {
        return x.call { y: Any? -> "OK" }
    }
    return "Fail"
}

// FILE: A.java
public class A<T> {
    public interface I<S> {
        String apply(S x);
    }

    public String call(I<T> block) { return block.apply(null); }
}

