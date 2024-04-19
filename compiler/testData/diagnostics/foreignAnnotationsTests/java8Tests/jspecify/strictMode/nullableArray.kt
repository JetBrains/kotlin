// ISSUE: KT-57996
// JSPECIFY_STATE: strict
// FILE: J.java
package p;

import org.jspecify.annotations.Nullable;

public interface J {
    int @Nullable [] maybeInts();
}

// FILE: test.kt
package p;

fun go(j: J): Any = <!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>j.maybeInts()<!>
