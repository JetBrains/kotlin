// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ForbidTypeAliasWithMissingDependencyType
// ISSUE: KT-70179

// MODULE: lib
// FILE: some/my/Ann.kt
package some.my;

annotation class Ann()

// MODULE: alias(lib)
// FILE: wrapper/KAnn.kt
package wrapper

import some.my.Ann

public typealias KAnn = Ann

// MODULE: app(alias)
// FILE: test.kt

import wrapper.<!UNRESOLVED_IMPORT!>KAnn<!>

@<!MISSING_DEPENDENCY_CLASS, NOT_AN_ANNOTATION_CLASS!>KAnn<!>
fun foo() {}

/* GENERATED_FIR_TAGS: annotationDeclaration, functionDeclaration, primaryConstructor, typeAliasDeclaration */
