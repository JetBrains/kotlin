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
    J { <!EXPECTED_PARAMETER_TYPE_MISMATCH!>s: String<!> -> s} // should be prohibited, because SAM value parameter has nullable type
    J { "" + it<!UNSAFE_CALL!>.<!>length }
    J { <!NULL_FOR_NONNULL_TYPE!>null<!> }
    J { <!TYPE_MISMATCH!>it?.length?.toString()<!> }

    J2 { <!EXPECTED_PARAMETER_TYPE_MISMATCH!>s: String<!> -> s}
    J2 { "" + it<!UNSAFE_CALL!>.<!>length }
    J2 { <!NULL_FOR_NONNULL_TYPE!>null<!> }
    J2 { <!TYPE_MISMATCH!>it?.length?.toString()<!> }
}
