// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-89475
// LANGUAGE: +FullValueClasses

// MODULE: baseLib
open class A

// MODULE: lib(baseLib)
interface B

class C : A(), B

// MODULE: main(lib)
value class Some(val x: C)

/* GENERATED_FIR_TAGS: classDeclaration, interfaceDeclaration, primaryConstructor, propertyDeclaration, value */
