// RUN_PIPELINE_TILL: FRONTEND
// DISABLE_NEXT_PHASE_SUGGESTION
// ISSUE: KT-90960
// JDK_KIND: FULL_JDK_11
// COMPARE_WITH_LIGHT_TREE

// MODULE: moduleA
// FILE: module-info.java
module moduleA {
    exports a;
}

// FILE: a/A.java
package a;

public class A {}

// MODULE: moduleB(moduleA)
// FILE: module-info.java
module moduleB {
    requires kotlin.stdlib;
}

// FILE: usage.kt
import a.A

fun usage() {
    <!JAVA_MODULE_DOES_NOT_DEPEND_ON_MODULE{LT}!>A<!>()
}

/* GENERATED_FIR_TAGS: functionDeclaration, javaFunction */
