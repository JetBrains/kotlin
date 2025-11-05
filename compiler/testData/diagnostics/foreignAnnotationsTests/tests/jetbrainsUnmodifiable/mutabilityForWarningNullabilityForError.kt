// NULLABILITY_ANNOTATIONS: @org.jetbrains.annotations.UnmodifiableView:warn
// DIAGNOSTICS: -UNUSED_PARAMETER

// FILE: org/jetbrains/annotations/UnmodifiableView.java
package org.jetbrains.annotations;

public @interface UnmodifiableView {}

// FILE: org/jetbrains/annotations/Nullable.java
package org.jetbrains.annotations;

public @interface Nullable {}

// FILE: org/jetbrains/annotations/NotNull.java
package org.jetbrains.annotations;

public @interface NotNull {}

// FILE: J.java
import java.util.List;
import org.jetbrains.annotations.*;

public class J {
    @UnmodifiableView
    @Nullable
    public static List<String> nullable() {
        return null;
    }

    @UnmodifiableView
    @NotNull
    public static List<String> notNull() {
        return null;
    }
}

// FILE: main.kt
fun main() {
    takeMutable(J.notNull())
    takeReadOnly(J.notNull())
    takeMutable(<!TYPE_MISMATCH!>J.nullable()<!>)
    takeReadOnly(<!TYPE_MISMATCH!>J.nullable()<!>)
}

fun takeMutable(l: MutableList<String>) {}
fun takeReadOnly(l: List<String>) {}
