// LATEST_LV_DIFFERENCE
// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-54866

// FILE: JavaEmptyAnno.java
public @interface JavaEmptyAnno {}

// FILE: JavaNonEmptyAnno.java
public @interface JavaNonEmptyAnno {
    String getQuery();
}

// FILE: KotlinAnnoClasses.kt
class KotlinEmptyAnnoRaw : <!FINAL_SUPERTYPE, INHERITING_AN_ANNOTATION_CLASS_ERROR!>JavaEmptyAnno<!>

class KotlinNonEmptyAnnoRaw : <!FINAL_SUPERTYPE, INHERITING_AN_ANNOTATION_CLASS_ERROR!>JavaNonEmptyAnno<!>

fun main() {
    KotlinEmptyAnnoRaw()
    KotlinNonEmptyAnnoRaw().<!UNRESOLVED_REFERENCE!>query<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, flexibleType, functionDeclaration, javaProperty, javaType */
