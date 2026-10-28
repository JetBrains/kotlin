// RUN_PIPELINE_TILL: FRONTEND
error class E1
error class E2

// The forward-referenced component is resolved on demand during the SUPER_TYPES phase,
// which leads back to the type alias that is currently being resolved.
typealias A = <!RECURSIVE_TYPEALIAS_EXPANSION!>E1 | B<!>
typealias B = <!RECURSIVE_TYPEALIAS_EXPANSION!>A<!>

typealias C = <!RECURSIVE_TYPEALIAS_EXPANSION!>E1 | D<!>
typealias D = <!RECURSIVE_TYPEALIAS_EXPANSION!>E2 | C<!>

typealias F = <!RECURSIVE_TYPEALIAS_EXPANSION!>Int | G<!>
typealias G = <!RECURSIVE_TYPEALIAS_EXPANSION!>H | E1<!>
typealias H = <!RECURSIVE_TYPEALIAS_EXPANSION!>F<!>

/* GENERATED_FIR_TAGS: classDeclaration, typeAliasDeclaration */
