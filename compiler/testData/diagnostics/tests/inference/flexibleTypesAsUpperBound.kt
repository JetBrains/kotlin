// FILE: 1.kt
fun <S> Array<S>.plus(): Array<S> {
    val result = Arrays.copyOf(this, 3)
    // result type is Array<(out) (S&Any)!>!
    return result
}

// FILE: Arrays.java
public class Arrays {
    public static <T> T[] copyOf(T[] original, int newLength) {
        return (T[]) null;
    }
}
