// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +CollectionLiterals
// FIR_DUMP
// ISSUE: KT-88744
// FULL_JDK

// FILE: p/JavaUtils.java
package p;

class JavaUtils {
    static java.util.List<String> list = new java.util.ArrayList<>();
    static java.util.Collection<String> collection = new java.util.ArrayList<>();
}

// FILE: p/main.kt
package p

fun main() {
    var list = JavaUtils.list
    list = ["1", "2", "3"] // mutableListOf

    var collection = JavaUtils.collection
    collection = ["1", "2", "3"] // listOf
}

/* GENERATED_FIR_TAGS: assignment, flexibleType, functionDeclaration, javaProperty, localProperty, propertyDeclaration,
stringLiteral */
