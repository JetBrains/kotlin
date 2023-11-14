// FIR_IDENTICAL
// JSPECIFY_STATE: warn

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
    <!WRONG_NULLABILITY_FOR_JAVA_OVERRIDE!>override<!> fun foo(): String? { return null }

    override fun bar(): String? { return null }
}

class DerivedNonNull : SomeJavaClass() {
    override fun foo(): String { return "" }

    override fun bar(): String { return "" }
}
