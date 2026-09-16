// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-83304, KT-89232
// FIR_DUMP
// WITH_STDLIB

enum class Something { A, B }

fun main() {
    val somethingList = mutableListOf<Something>()

    somethingList.add(A)
    somethingList += A
    somethingList -= A
    somethingList.plusAssign(A)
    somethingList.minusAssign(A)
    somethingList += Something.A

    val nullableList = mutableListOf<Something?>()
    nullableList += A
    nullableList.add(A)

    val listOfSets = mutableListOf<Set<Something>>()
    // CSR here is used againts type variable from `setOf`, so we don't run it until completion.
    // Thus, we're not able to choose between `plusAssign` accepting a single or a Collection of elements.
    // See KT-89507
    <!VAL_REASSIGNMENT!>listOfSets<!> += <!ASSIGNMENT_TYPE_MISMATCH!>setOf<!>(<!UNRESOLVED_REFERENCE!>A<!>)
    listOfSets.plusAssign(<!CANNOT_INFER_PARAMETER_TYPE!>setOf<!>(<!UNRESOLVED_REFERENCE!>A<!>))
    listOfSets += setOf(Something.A)
}

/* GENERATED_FIR_TAGS: additiveExpression, assignment, enumDeclaration, enumEntry, functionDeclaration, localProperty,
nullableType, propertyDeclaration */
