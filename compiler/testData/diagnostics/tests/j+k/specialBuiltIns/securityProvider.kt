// RUN_PIPELINE_TILL: CODEGEN
// FULL_JDK

import java.security.Provider

class Example : Provider("A", 1.0, "B")

/* GENERATED_FIR_TAGS: classDeclaration, stringLiteral */
