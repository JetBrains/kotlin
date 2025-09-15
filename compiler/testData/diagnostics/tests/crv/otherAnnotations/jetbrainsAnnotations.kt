// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

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
    Usage().method()
    KotlinUsage().method()
}

/* GENERATED_FIR_TAGS: classDeclaration, flexibleType, functionDeclaration, javaFunction, javaType, stringLiteral */
