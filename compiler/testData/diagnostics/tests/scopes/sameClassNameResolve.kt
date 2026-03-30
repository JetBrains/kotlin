// RUN_PIPELINE_TILL: BACKEND
// FILE: SameClassNameResolveTest.kt
package test

open class Base
class SubBase: Base()

// FILE: SameClassNameResolveRoot.kt

open class Base
class SubBase: Base()

/* GENERATED_FIR_TAGS: classDeclaration */
