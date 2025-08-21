// WITH_KOTLIN_JVM_ANNOTATIONS
// FILE: AnnotatedFlexibleTypes.kt
package test

public class AnnotatedFlexibleTypes(val javaClass: d.JavaClass) {
    fun foo() = javaClass.foo()

    val bar = javaClass.bar()
}

// FILE: d/JavaClass.java
package d;

import org.jetbrains.annotations.*;
import kotlin.annotations.jvm.*;

public abstract class JavaClass {
    @NotNull
    @Nullable
    public abstract Integer foo();

    @Mutable
    @ReadOnly
    public abstract java.util.Collection<Integer> bar();
}
