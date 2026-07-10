// RUN_PIPELINE_TILL: BACKEND
// FILE: a/x.java
package a;

// A class that declares BOTH a type parameter `T` and an (own) nested class `T` with the very
// same simple name. `getT()`'s return type is written as the bare name `T`.
//
// javac resolves the bare `T` to the nested class `a.x.T`, while the Kotlin
// compiler (both the PSI-based Java resolution and `java-direct`) resolves it to the OWN TYPE
// PARAMETER `T` first.
public class x<T extends CharSequence> {

    public static class T {}

    public T getT() { return null; }

}

// FILE: test.kt
package test

import a.*

fun test() = x<String>().getT().length

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, javaFunction, javaType */
