// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER
// JSPECIFY_STATE warn
// FILE: A.java

import org.jspecify.annotations.*;

@DefaultNonNull
public class A {
    public String defaultField = "";
    @Nullable public String field = null;

    public String everythingNotNullable(String x) { return ""; }

    @DefaultNullable
    public String everythingNullable(String x) { return ""; }

    @DefaultNullnessUnspecified
    public String everythingUnknown(String x) { return ""; }

    @DefaultNullable
    public String mixed(@NotNull String x) { return ""; }

    public String explicitlyNullnessUnspecified(@NullnessUnspecified String x) { return ""; }
}

// FILE: main.kt

fun main(a: A) {
    a.everythingNotNullable(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>)<!UNNECESSARY_SAFE_CALL!>?.<!>length
    a.everythingNotNullable(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>).length
    a.everythingNotNullable("").length

    <!RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>a.everythingNullable(null)<!>.length
    a.everythingNullable(null)?.length

    a.everythingUnknown(null).length
    a.everythingUnknown(null)?.length

    <!RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>a.mixed(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>)<!>.length
    a.mixed(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>)?.length
    a.mixed("")?.length

    a.explicitlyNullnessUnspecified("").length
    a.explicitlyNullnessUnspecified("")<!UNNECESSARY_SAFE_CALL!>?.<!>length
    a.explicitlyNullnessUnspecified(null).length

    a.defaultField<!UNNECESSARY_SAFE_CALL!>?.<!>length
    a.defaultField.length

    a.field?.length
    <!RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>a.field<!>.length
}
