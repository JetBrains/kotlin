// FILE: Derived.java
import kotlin.jvm.functions.Function0;
import org.jetbrains.annotations.NotNull;

public class Derived implements Base {
    @Override
    public <V> void foo(@NotNull Function0<? extends V> compute) {}
}

// FILE: main.kt
interface Base {
    fun <V: Any> foo(compute: () -> V?)
}

fun main() {
    Derived().foo<String> { "" }
}
