// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER
// CODE_ANALYSIS_STATE warn
// FILE: A.java

import org.jspecify.annotations.*;

public class A<T extends @NotNull Object, E extends @Nullable Object, F extends @NullnessUnknown Object> {
}

// FILE: B.java

import org.jspecify.annotations.*;

@DefaultNullable
public class B {
    public void bar(A<String, String, String> a) {}
}

// FILE: C.java

import org.jspecify.annotations.*;

@DefaultNotNull
public class C {
    public void bar(A<String, String, String> a) {}
}

// FILE: D.java

import org.jspecify.annotations.*;

@DefaultNullnessUnknown
public class D {
    public void bar(A<String, String, String> a) {}
}

// FILE: main.kt

fun main(
    aNotNullNotNullNotNull: A<String, String, String>,
    aNotNullNotNullNull: A<String, String, String?>,
    aNotNullNullNotNull: A<String, String?, String>,
    aNotNullNullNull: A<String, String?, String?>,
    b: B, c: C, d: D
) {
    // TODO: NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS should be reported
    b.bar(aNotNullNotNullNotNull)
    // TODO: NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS should be reported
    b.bar(aNotNullNotNullNull)
    // TODO: NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS should be reported
    b.bar(aNotNullNullNotNull)
    b.bar(aNotNullNullNull)

    c.bar(aNotNullNotNullNotNull)
    // TODO: NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS should be reported
    c.bar(aNotNullNotNullNull)
    // TODO: NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS should be reported
    c.bar(aNotNullNullNotNull)
    // TODO: NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS should be reported
    c.bar(aNotNullNullNull)

    // TODO: NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS should be reported
    d.bar(aNotNullNotNullNotNull)
    // TODO: NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS should be reported
    d.bar(aNotNullNotNullNull)
    d.bar(aNotNullNullNotNull)
    d.bar(aNotNullNullNull)
}
