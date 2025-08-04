// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: -ForbidUnrelatedMissingSupertypes
// ISSUE: KT-79789

// MODULE: platform-core
// FILE: PsiFile.java
public interface PsiFile {
    PsiFile getSelf();
}

// MODULE: css-common
// FILE: StylesheetFileMarker.java
public interface StylesheetFileMarker {}

// MODULE: css(platform-core, css-common)
// FILE: StylesheetFile.java
public interface StylesheetFile extends PsiFile, StylesheetFileMarker {}

// MODULE: main(platform-core, css)
fun test(file: StylesheetFile) {
    file.<!MISSING_DEPENDENCY_SUPERCLASS_WARNING("StylesheetFileMarker; StylesheetFile")!>self<!>
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, ifExpression, isExpression, javaProperty, smartcast */
