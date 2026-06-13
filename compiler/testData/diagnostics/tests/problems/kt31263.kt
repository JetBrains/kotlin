// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-31263
// WITH_STDLIB

// FILE: Sam.java
import java.util.List;

public interface Sam {
    List<String> test();
}

// FILE: main.kt

// KT-31263: Update result type of lambda after analyzing of it's body
// The expected type for the lambda body is flexible List<String>!, which
// should not be incorrectly approximated to MutableList<String>?
fun main() {
    val s1 = Sam { listOf("") }

    val s2 = object : Sam {
        override fun test(): List<String> {
            return listOf("")
        }
    }
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, flexibleType, functionDeclaration, javaType, lambdaLiteral,
localProperty, override, propertyDeclaration, stringLiteral */
