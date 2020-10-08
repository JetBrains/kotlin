// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER
// JSPECIFY_STATE warn
// FILE: A.java

import org.jspecify.annotations.*;

public class A {
    @Nullable public String field = null;

    @Nullable
    public String foo(@NotNull String x, @NullnessUnspecified CharSequence y) {
        return "";
    }

    @NotNull
    public String bar() {
        return "";
    }

}

// FILE: main.kt

fun main(a: A) {
    a.foo("", null)?.length
    <!RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>a.foo("", null)<!>.length
    <!RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>a.foo(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>, "")<!>.length

    a.bar().length
    a.bar()<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>.length

    a.field?.length
    <!RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>a.field<!>.length
}
