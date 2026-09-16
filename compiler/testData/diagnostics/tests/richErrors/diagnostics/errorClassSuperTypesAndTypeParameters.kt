// RUN_PIPELINE_TILL: FRONTEND
// ALLOW_KOTLIN_PACKAGE
// FILE: RichError.kt
package kotlin

abstract class RichError
// FILE: test.kt
interface I
open class C

error class E1 : <!ERROR_CLASS_HAS_SUPERTYPE!>I<!>
error class E2 : <!ERROR_CLASS_HAS_SUPERTYPE!>C<!>()
error class E3 : <!ERROR_CLASS_HAS_SUPERTYPE!>Any<!>()

error class E4<<!ERROR_CLASS_HAS_TYPE_PARAMETER!>T<!>>
error class E5<<!ERROR_CLASS_HAS_TYPE_PARAMETER!>T<!>, <!ERROR_CLASS_HAS_TYPE_PARAMETER!>R : Any<!>>

error object O1 : <!ERROR_CLASS_HAS_SUPERTYPE!>I<!>
error object O2 : <!ERROR_CLASS_HAS_SUPERTYPE!>C<!>()
error object O3 : <!ERROR_CLASS_HAS_SUPERTYPE!>Any<!>()

/* GENERATED_FIR_TAGS: classDeclaration, interfaceDeclaration, objectDeclaration */
