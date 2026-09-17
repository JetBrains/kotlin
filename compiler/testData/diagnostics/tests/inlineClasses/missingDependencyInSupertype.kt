// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-89475
// WITH_STDLIB

// MODULE: baseLib
open class A

// MODULE: lib(baseLib)
interface B

class C : A(), B

// MODULE: main(lib)
@JvmInline
value class Some(val x: C)

/* GENERATED_FIR_TAGS: classDeclaration, interfaceDeclaration, primaryConstructor, propertyDeclaration, value */
