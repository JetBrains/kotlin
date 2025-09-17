// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

// FILE: usage/Usage.java
package usage;

import org.jetbrains.annotations.*;

public class Usage {
    @Contract(pure = true)
    public String pure() {
        return "";
    }

    @Contract("null -> null")
    public String notPure(String arg) {
        return arg;
    }

    @Contract(value = "null -> null", pure = true)
    public String both(String arg) {
        return arg;
    }
}

// FILE: KotlinUsage.kt
package usage

import org.jetbrains.annotations.*

class KotlinUsage {
    @Contract(pure = true) fun pure() = ""
    @Contract("null -> null") fun notPure(s: String) = s
    @Contract(value = "null -> null", pure = true) fun both(s: String) = s
}

fun usage() {
    Usage().pure()
    Usage().notPure("")
    Usage().both("")

    KotlinUsage().pure()
    KotlinUsage().notPure("")
    KotlinUsage().both("")
}

/* GENERATED_FIR_TAGS: classDeclaration, flexibleType, functionDeclaration, javaFunction, javaType, stringLiteral */
