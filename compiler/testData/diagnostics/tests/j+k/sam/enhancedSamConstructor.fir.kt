// !WITH_NEW_INFERENCE
// FILE: J.java
import org.jetbrains.annotations.*;

public interface J {
    @NotNull
    String foo(@Nullable String x);
}

// FILE: J2.java
import org.jetbrains.annotations.Nullable;

public interface J2 extends J {
    String foo(String x);
}

// FILE: main.kt
fun main() {
    J { s: String -> <!ARGUMENT_TYPE_MISMATCH!>s<!>} // should be prohibited, because SAM value parameter has nullable type
    J { "" + it<!UNSAFE_CALL!>.<!>length }
    J { null }
    J { it?.length?.toString() }

    J2 { s: String -> <!ARGUMENT_TYPE_MISMATCH!>s<!>}
    J2 { "" + it<!UNSAFE_CALL!>.<!>length }
    J2 { null }
    J2 { it?.length?.toString() }
}
