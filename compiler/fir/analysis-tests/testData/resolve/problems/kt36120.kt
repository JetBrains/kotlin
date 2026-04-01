// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-36120

// KT-36120: Resolve for badly parsed typealias provokes a non-local error
typealias A = java.util.Date
typealias java<!SYNTAX!>.<!><!UNRESOLVED_REFERENCE!>util<!>.Date

/* GENERATED_FIR_TAGS: typeAliasDeclaration */
