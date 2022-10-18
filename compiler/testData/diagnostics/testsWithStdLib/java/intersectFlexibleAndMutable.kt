// FIR_IDENTICAL
// FILE: Util.java
import java.util.List;

public class Util {
    public static <T> List<T> id(List<T> x) { return x; }
}

// FILE: main.kt
fun main() {
    var list = mutableListOf(1)
    list = Util.id(list)
    list += 2
}
