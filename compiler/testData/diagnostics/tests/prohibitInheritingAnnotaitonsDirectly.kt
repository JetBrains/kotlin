// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-54866
// LANGUAGE: +ProhibitExtendingAnnotationClasses

// FILE: JavaEmptyAnno.java
public @interface JavaEmptyAnno {}

// FILE: JavaNonEmptyAnno.java
public @interface JavaNonEmptyAnno {
    String getQuery();
}

// FILE: KotlinAnnoClasses.kt
class KotlinEmptyAnnoRaw : <!EXTENDING_AN_ANNOTATION_CLASS_ERROR, FINAL_SUPERTYPE!>JavaEmptyAnno<!>

class KotlinNonEmptyAnnoRaw : <!EXTENDING_AN_ANNOTATION_CLASS_ERROR, FINAL_SUPERTYPE!>JavaNonEmptyAnno<!>

fun main() {
    KotlinEmptyAnnoRaw()
    KotlinNonEmptyAnnoRaw().<!UNRESOLVED_REFERENCE!>query<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, flexibleType, functionDeclaration, javaProperty, javaType */
