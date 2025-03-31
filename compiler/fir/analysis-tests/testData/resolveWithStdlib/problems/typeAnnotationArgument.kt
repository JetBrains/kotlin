// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-76381

@Target(AnnotationTarget.TYPE)
annotation class Anno(val value: Int)

fun check(): List<@Anno(<!UNRESOLVED_REFERENCE!>CONST<!>)<!SYNTAX!><!>>? = null
