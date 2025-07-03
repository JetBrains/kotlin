// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

// FILE: com/google/errorprone/annotations/CheckReturnValue.java
package com.google.errorprone.annotations;

public @interface CheckReturnValue {}

// FILE: com/google/errorprone/annotations/CanIgnoreReturnValue.java
package com.google.errorprone.annotations;

public @interface CanIgnoreReturnValue {}

// FILE: usage/Usage.java
package usage;

import com.google.errorprone.annotations.*;

@CheckReturnValue
public class Usage {
    public String method() {
        return "";
    }

    @CanIgnoreReturnValue
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

fun usage() {
    <!RETURN_VALUE_NOT_USED!>Usage().method()<!>
    Usage().ignored()
    <!RETURN_VALUE_NOT_USED!>KotlinUsage().method()<!>
    KotlinUsage().ignored()
}

/* GENERATED_FIR_TAGS: classDeclaration, flexibleType, functionDeclaration, javaFunction, javaType, stringLiteral */
