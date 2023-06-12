// FIR_IDENTICAL
// FULL_JDK

// FILE: JavaClass.java
import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;

public class JavaClass {
    public static <E> void consume(@NotNull E value) {}
    public static <E> void compute(Supplier<@NotNull E> computable) {}
}

// FILE: KotlinMain.kt
fun test1() {
    JavaClass.compute { 42.apply {} }
}

fun test2() {
    JavaClass.compute { if (true) 42 else 42 }
}

fun test3() {
    42.apply(JavaClass::consume)
}
