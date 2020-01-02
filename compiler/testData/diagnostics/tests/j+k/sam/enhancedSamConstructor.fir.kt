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
    <!INAPPLICABLE_CANDIDATE!>J<!> { s: String -> s} // should be prohibited, because SAM value parameter has nullable type
    J { "" + it.<!INAPPLICABLE_CANDIDATE!>length<!> }
    J { null }
    J { it?.length?.toString() }

    <!INAPPLICABLE_CANDIDATE!>J2<!> { s: String -> s}
    J2 { "" + it.<!INAPPLICABLE_CANDIDATE!>length<!> }
    J2 { null }
    J2 { it?.length?.toString() }
}
