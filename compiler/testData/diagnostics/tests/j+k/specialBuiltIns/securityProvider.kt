// RUN_PIPELINE_TILL: BACKEND
// FULL_JDK
// DIAGNOSTICS: -DEPRECATION
// ^LL runners use an older JDK, where `Provider` isn't deprecated.

import java.security.Provider

class Example : Provider("A", 1.0, "B")

/* GENERATED_FIR_TAGS: classDeclaration, stringLiteral */
