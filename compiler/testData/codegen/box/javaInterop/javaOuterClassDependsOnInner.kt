// TARGET_BACKEND: JVM
// WITH_STDLIB
// FILE: Layout.java
import org.jetbrains.annotations.NotNull;

public class Layout<T> {
    public static <V> Layout<V> singleOf(@NotNull final Class<V> valueClass) {
        return null;
    }
}
// FILE: main.kt

fun foo() {
    val x = Layout.singleOf<String?>(String::class.java)
}


fun box() = "OK"
