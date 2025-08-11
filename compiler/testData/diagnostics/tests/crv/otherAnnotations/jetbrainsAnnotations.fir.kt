// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

// FILE: org/jetbrains/annotations/CheckReturnValue.java
package org.jetbrains.annotations;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.TYPE, ElementType.PACKAGE})
public @interface CheckReturnValue {}

// FILE: usage/Usage.java
package usage;

import org.jetbrains.annotations.*;

@CheckReturnValue
public class Usage {
    public String method() {
        return "";
    }
}

// FILE: KotlinUsage.kt
package usage

import org.jetbrains.annotations.*

@CheckReturnValue
class KotlinUsage {
    fun method() = ""
}

fun usage() {
    <!RETURN_VALUE_NOT_USED!>Usage().method()<!>
    <!RETURN_VALUE_NOT_USED!>KotlinUsage().method()<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, flexibleType, functionDeclaration, javaFunction, javaType, stringLiteral */
