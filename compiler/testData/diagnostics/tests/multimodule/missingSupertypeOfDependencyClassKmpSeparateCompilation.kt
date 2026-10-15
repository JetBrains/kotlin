// RUN_PIPELINE_TILL: FRONTEND
// DISABLE_NEXT_PHASE_SUGGESTION
// LANGUAGE: +MultiPlatformProjects
// SEPARATE_KMP_COMPILATION
// ISSUE: KT-89769

// MODULE: base-common
// FILE: base.kt
interface Base

// MODULE: lib-common(base-common)
// FILE: lib.kt
interface Visible

class Lib : Visible, Base

// MODULE: app-common(lib-common)
// FILE: app-common.kt
fun <!MISSING_DEPENDENCY_SUPERCLASS{PLATFORM}!>Lib<!>.use(<!MISSING_DEPENDENCY_SUPERCLASS{PLATFORM}!>lib: Lib<!>): <!MISSING_DEPENDENCY_SUPERCLASS{PLATFORM}!>Lib<!> = lib

/* GENERATED_FIR_TAGS: interfaceDeclaration */
