// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-83304, KT-89232
// FIR_DUMP
// WITH_STDLIB

enum class Something { A, B }

fun main() {
    val somethingList = mutableListOf<Something>()

    somethingList.add(A)
    somethingList += <!UNRESOLVED_REFERENCE!>A<!>
    somethingList -= <!UNRESOLVED_REFERENCE!>A<!>
    somethingList.plusAssign(<!UNRESOLVED_REFERENCE!>A<!>)
    somethingList.minusAssign(<!UNRESOLVED_REFERENCE!>A<!>)
    somethingList += Something.A

    val nullableList = mutableListOf<Something?>()
    nullableList += <!UNRESOLVED_REFERENCE!>A<!>
    nullableList.add(A)

    val listOfSets = mutableListOf<Set<Something>>()
    <!VAL_REASSIGNMENT!>listOfSets<!> += <!ASSIGNMENT_TYPE_MISMATCH!>setOf<!>(<!UNRESOLVED_REFERENCE!>A<!>)
    listOfSets.plusAssign(<!CANNOT_INFER_PARAMETER_TYPE!>setOf<!>(<!UNRESOLVED_REFERENCE!>A<!>))
    listOfSets += setOf(Something.A)
}

/* GENERATED_FIR_TAGS: additiveExpression, assignment, enumDeclaration, enumEntry, functionDeclaration, localProperty,
nullableType, propertyDeclaration */
