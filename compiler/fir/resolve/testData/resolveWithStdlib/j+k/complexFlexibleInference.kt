// FILE: BindingContext.java
public interface BindingContext {
    @org.jetbrains.annotations.Nullable
    <K, V> V get(ReadOnlySlice<K, V> slice, K key);
}

// FILE: ReadOnlySlice.java
public interface ReadOnlySlice<K, V> {}

// FILE: Slices.java

public class Slices {
    public static ReadOnlySlice<String, Double> X = null;
    public static ReadOnlySlice<Integer, String> Y = null;
}

// FILE: main.kt

fun bar(bindingContext: BindingContext) {
    bindingContext[Slices.X, bindingContext[Slices.Y, 1]]
}
