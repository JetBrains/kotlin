// RUN_PIPELINE_TILL: FRONTEND
// TARGET_BACKEND: JVM
// ISSUE: KT-74501
// LANGUAGE: +ContextParameters

// FILE: KotlinContextInterface.kt
package org

interface KotlinContextInterface {
    context(a: String)
    fun foo(b: String): String
}

// FILE: JavaClass.java
import org.KotlinContextInterface;

public class JavaClass implements KotlinContextInterface {
    @Override
    public String foo(String a, String b) {
        return a + b;
    }
}

// FILE: test.kt

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class KJK<!>: JavaClass()