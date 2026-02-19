// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-78909

// CAPS is a typealias, Usual is a class
// --> is subtyping/aliasing edge, **> is type arguments edge
// Erroneous loop should include either typealiases only or subtyping/aliasing edges only

// T **> A --> S   Loop of(A, S) includes only subtyping/aliasing edges ==> error
//       ^-----|
typealias T = A.() -> Unit

class A() : <!CYCLIC_INHERITANCE_HIERARCHY, FINAL_SUPERTYPE, SUPERTYPE_NOT_INITIALIZED!>S<!> {}

typealias S = A

/* GENERATED_FIR_TAGS: classDeclaration, functionalType, primaryConstructor, typeAliasDeclaration, typeWithExtension */
