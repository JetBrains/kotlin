// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

// FILE: org/jooq/CheckReturnValue.java
package org.jooq;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
public @interface CheckReturnValue {}

// FILE: usage/Usage.java
package usage;

import org.jooq.*;

public class Usage {
    @CheckReturnValue
    public String method() {
        return "";
    }
}

// FILE: KotlinUsage.kt
package usage

import org.jooq.*

class KotlinUsage {
    @CheckReturnValue
    fun method() = ""
}

fun usage() {
    Usage().<!RETURN_VALUE_NOT_USED!>method<!>()
    KotlinUsage().<!RETURN_VALUE_NOT_USED!>method<!>()
}

/* GENERATED_FIR_TAGS: classDeclaration, flexibleType, functionDeclaration, javaFunction, javaType, stringLiteral */
