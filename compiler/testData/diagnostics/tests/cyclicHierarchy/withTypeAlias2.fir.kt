// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-78909

typealias T = A.() -> Unit

class A() : <!CYCLIC_INHERITANCE_HIERARCHY!>S<!> {}

typealias S = <!RECURSIVE_TYPEALIAS_EXPANSION!>A<!>

/* GENERATED_FIR_TAGS: classDeclaration, functionalType, primaryConstructor, typeAliasDeclaration, typeWithExtension */
