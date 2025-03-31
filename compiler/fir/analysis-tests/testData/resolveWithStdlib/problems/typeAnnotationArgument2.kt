// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-76381
// FIR_DUMP

@Target(AnnotationTarget.TYPE)
annotation class Anno(val value: Int)

interface MyList : List<@Alias(<!UNRESOLVED_REFERENCE!>CONST<!>)<!SYNTAX!><!>>

interface MyListProper : List<@Alias(CORRECT) String>

const val CORRECT = 2 + 2

typealias Alias = Anno
