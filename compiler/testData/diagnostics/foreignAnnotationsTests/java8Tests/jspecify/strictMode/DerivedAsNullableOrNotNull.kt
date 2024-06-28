// FIR_IDENTICAL
// JSPECIFY_STATE: strict

// FILE: SomeJavaClass.java

import org.jspecify.annotations.*;

public class SomeJavaClass {
    @NonNull
    public String foo() { return ""; }

    @Nullable
    public String bar() { return ""; }
}

// FILE: test.kt

class DerivedNullable : SomeJavaClass() {
    // jspecify_nullness_mismatch
    override fun foo(): <!RETURN_TYPE_MISMATCH_ON_OVERRIDE!>String?<!> { return null }

    override fun bar(): String? { return null }
}

class DerivedNonNull : SomeJavaClass() {
    override fun foo(): String { return "" }

    override fun bar(): String { return "" }
}
