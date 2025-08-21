// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-78909

// CAPS is a typealias, Usual is a class
// --> is subtyping/aliasing edge, **> is type arguments edge
// Erroneous loop should include either typealiases only or subtyping/aliasing edges only

// T **> A ---|   Loop of(A) includes only subtyping/aliasing edges ==> error
//       ^----|
typealias T = A.() -> Unit

class A() : <!CYCLIC_INHERITANCE_HIERARCHY, FINAL_SUPERTYPE, SUPERTYPE_NOT_INITIALIZED!>A<!> {}

/* GENERATED_FIR_TAGS: classDeclaration, primaryConstructor */
