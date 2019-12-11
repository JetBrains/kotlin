// FILE: Derived.java
import kotlin.jvm.functions.Function0;
import org.jetbrains.annotations.NotNull;

public class Derived implements Base {
    @Override
    public <V> void foo() {}
}

// FILE: main.kt
interface Base {
    fun <V: Any> foo()
}

class KotlinDerived1 : Derived() {
    override fun <V: Any?> foo() {}
}

class KotlinDerived2 : Derived() {
    override fun <V: Any> foo() {}
}

fun main() {
    Derived().foo<String>()
}
