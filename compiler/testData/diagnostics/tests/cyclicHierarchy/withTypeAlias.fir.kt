// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-78909

typealias T = A.() -> Unit

class A() : <!CYCLIC_INHERITANCE_HIERARCHY!>A<!> {}

/* GENERATED_FIR_TAGS: classDeclaration, primaryConstructor */
