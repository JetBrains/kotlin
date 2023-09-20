// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER
// JSR305_GLOBAL_REPORT: warn

// FILE: test/package-info.java
@javax.annotation.ParametersAreNonnullByDefault()
package test;

// FILE: test/A.java
package test;

import javax.annotation.*;

public class A {
    @Nullable public String field = null;

    public String foo(String q, @Nonnull String x, @CheckForNull CharSequence y) {
        return "";
    }

    @Nonnull
    public String bar() {
        return "";
    }
}

// FILE: test2/A2.java
package test2;

import javax.annotation.*;

public class A2 {
    @Nullable public String field = null;

    public String foo(String q, @Nonnull String x, @CheckForNull CharSequence y) {
        return "";
    }

    @Nonnull
    public String bar() {
        return "";
    }
}

// FILE: main.kt
import test.A
import test2.A2

fun main(a: A, a2: A2) {
    a.foo("", "", null)?.length
    a.foo("", "", null).length
    a.foo(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>, <!NULL_FOR_NONNULL_TYPE!>null<!>, "").length

    a.bar().length
    a.bar()<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>.length

    a.field?.length
    a.field<!UNSAFE_CALL!>.<!>length

    a2.foo("", "", null)?.length
    a2.foo("", "", null).length
    a2.foo(null, <!NULL_FOR_NONNULL_TYPE!>null<!>, "").length

    a2.bar().length
    a2.bar()<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>.length

    a2.field?.length
    a2.field<!UNSAFE_CALL!>.<!>length
}
