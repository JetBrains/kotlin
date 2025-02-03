// RUN_PIPELINE_TILL: BACKEND
// TARGET_BACKEND: JVM
// ISSUE: KT-74501
// LANGUAGE: +ContextParameters
// SCOPE_DUMP: KJK:foo;bar;baz;qux;quux

// FILE: KotlinContextInterface.kt
package org

interface KotlinContextInterface {
    context(a: String)
    fun foo(b: Int): String

    context(a: String)
    fun Int.bar(b: Boolean): String

    context(a: String)
    fun baz(): String

    context(a: String, b: String)
    fun qux(): String

    context(a: String, b: String)
    fun quux(c: String): String
}

interface KotlinInterface2 {
    context(a: String, b: Int)
    fun foo(): String

    context(a: String, b: Int, c: Boolean)
    fun bar(): String

    fun baz(a: String): String

    context(a: String, b: String)
    fun qux(): String

    context(a: String, b: String)
    fun String.quux(): String
}

interface KotlinInterface3 {
    fun String.baz(): String
}

// FILE: JavaClass.java
import org.KotlinContextInterface;
import org.KotlinInterface2;
import org.KotlinInterface3;

public class JavaClass implements KotlinContextInterface, KotlinInterface2, KotlinInterface3 {
    @Override
    public String foo(String a, int b) {
        return a;
    }

    @Override
    public String bar(String a, int b, boolean c) {
        return a;
    }

    @Override
    public String baz(String a) {
        return a;
    }

    @Override
    public String qux(String a, String b) {
        return a;
    }

    @Override
    public String quux(String a, String b, String c) {
        return a;
    }
}

// FILE: test.kt

class KJK : JavaClass()
