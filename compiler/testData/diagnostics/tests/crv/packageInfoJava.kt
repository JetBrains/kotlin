// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

// FILE: com/google/errorprone/annotations/CheckReturnValue.java
package com.google.errorprone.annotations;

public @interface CheckReturnValue {}

// FILE: com/google/errorprone/annotations/CanIgnoreReturnValue.java
package com.google.errorprone.annotations;

public @interface CanIgnoreReturnValue {}

// FILE: usage/package-info.java

@com.google.errorprone.annotations.CheckReturnValue
package usage;

// FILE: usage/Usage.java

package usage;

public class Usage {
    public String method() {
        return "";
    }

    @com.google.errorprone.annotations.CanIgnoreReturnValue
    public String ignored() {
        return "";
    }
}

// FILE: KotlinUsage.kt
package usage

import com.google.errorprone.annotations.*

@CheckReturnValue
class KotlinUsage {
    fun method() = ""
    @CanIgnoreReturnValue fun ignored() = ""
}

class NonAnnotated {
    fun method() = ""
}

fun usage() {
    Usage().method()
    Usage().ignored()
    KotlinUsage().method()
    KotlinUsage().ignored()
    NonAnnotated().method()
}

/* GENERATED_FIR_TAGS: classDeclaration, flexibleType, functionDeclaration, javaFunction, javaType, stringLiteral */
