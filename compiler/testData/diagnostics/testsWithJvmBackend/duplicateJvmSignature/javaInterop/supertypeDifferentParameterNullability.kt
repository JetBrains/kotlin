// FIR_IDENTICAL
// FILE: A.java
import org.jetbrains.annotations.*;

public interface A {
    void foo(@Nullable String x);
}

// FILE: B.java
import org.jetbrains.annotations.*;

public interface B {
    void foo(@NotNull String x);
}

// FILE: C.kt

<!CONFLICTING_INHERITED_JVM_DECLARATIONS!>interface I : A, B<!>
