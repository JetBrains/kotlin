// RUN_PIPELINE_TILL: FRONTEND
error class E1
error class E2

typealias A = <!RECURSIVE_TYPEALIAS_EXPANSION!>B | E1<!>
typealias B = <!RECURSIVE_TYPEALIAS_EXPANSION!>A | E2<!>

typealias C = <!RECURSIVE_TYPEALIAS_EXPANSION!>Int | D<!>
typealias D = <!RECURSIVE_TYPEALIAS_EXPANSION!>C | E1<!>

typealias L = <!RECURSIVE_TYPEALIAS_EXPANSION!>List<L> | E1<!>
/* GENERATED_FIR_TAGS: classDeclaration, typeAliasDeclaration */
