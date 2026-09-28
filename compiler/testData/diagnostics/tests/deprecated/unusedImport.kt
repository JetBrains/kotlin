// RUN_PIPELINE_TILL: CODEGEN
// FILE: A.kt

package test

@Deprecated("A")
interface A

// FILE: B.kt

import test.A

/* GENERATED_FIR_TAGS: interfaceDeclaration, stringLiteral */
