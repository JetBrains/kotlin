// FIR_IDENTICAL
// FILE: Box.java
import org.jetbrains.annotations.NotNull;

public class Box<T> {
    public void put(@NotNull T t) {}
}

// FILE: IntBox.java
import org.jetbrains.annotations.NotNull;

public class IntBox extends Box<Integer> {
    public int result = 0;
    @Override
    public void put(@NotNull Integer t) {
        result = t;
    }
}

// FILE: main.kt
fun main() {
    IntBox().put(1)
}
