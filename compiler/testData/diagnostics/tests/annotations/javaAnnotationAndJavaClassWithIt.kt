// FIR_IDENTICAL
// ISSUE: KT-56847
// FILE: foo/TestTarget.java
package foo;

@AnnotationWithArg(String.class)
@Ann
public final class TestTarget {}

// FILE: foo/Ann.java
package foo;

public @interface Ann {}

// FILE: foo/AnnotationWithArg.java
package foo;

public @interface AnnotationWithArg {
    Class<?> value();
}

// FILE: foo/AnotherTarget.kt
package foo

@Ann
class AnotherTarget {
    fun hello() {}
}
