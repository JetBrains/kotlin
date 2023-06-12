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
    JavaClass.compute { <!ARGUMENT_TYPE_MISMATCH!>42.apply {}<!> }
}

fun test2() {
    JavaClass.compute { <!ARGUMENT_TYPE_MISMATCH!>if (true) 42 else 42<!> }
}

fun test3() {
    42.<!INAPPLICABLE_CANDIDATE!>apply<!>(JavaClass::<!UNRESOLVED_REFERENCE!>consume<!>)
}
