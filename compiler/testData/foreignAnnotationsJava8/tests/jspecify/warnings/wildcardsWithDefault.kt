// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER
// JSPECIFY_STATE warn
// FILE: A.java

import org.jspecify.annotations.*;

public class A<T extends @NotNull Object, E extends @Nullable Object, F extends @NullnessUnknown Object> {
}

// FILE: B.java

import org.jspecify.annotations.*;

public class B {
    @DefaultNotNull
    public void noBoundsNotNull(A<?, ?, ?> a) {}
    @DefaultNullable
    public void noBoundsNullable(A<?, ?, ?> a) {}
}

// FILE: main.kt

fun main(
    aNotNullNotNullNotNull: A<String, String, String>,
    aNotNullNotNullNull: A<String, String, String?>,
    aNotNullNullNotNull: A<String, String?, String>,
    aNotNullNullNull: A<String, String?, String?>,
    b: B
) {
    b.noBoundsNotNull(aNotNullNotNullNotNull)
    // TODO: NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS should be reported
    b.noBoundsNotNull(aNotNullNotNullNull)
    // TODO: NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS should be reported
    b.noBoundsNotNull(aNotNullNullNotNull)
    // TODO: NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS should be reported
    b.noBoundsNotNull(aNotNullNullNull)

    b.noBoundsNullable(aNotNullNotNullNotNull)
    b.noBoundsNullable(aNotNullNotNullNull)
    b.noBoundsNullable(aNotNullNullNotNull)
    b.noBoundsNullable(aNotNullNullNull)
}
