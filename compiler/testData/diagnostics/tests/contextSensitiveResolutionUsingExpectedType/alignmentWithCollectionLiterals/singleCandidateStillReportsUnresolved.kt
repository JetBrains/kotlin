// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-89232
// FIR_DUMP

enum class MyEnum3 { X, Y }

fun bar(x: MyEnum3) {}
fun barTwo(x: MyEnum3, y: MyEnum3) {}
fun <T> id(x: T): T = x

fun main() {
    bar(<!UNRESOLVED_REFERENCE!>Z<!>)
    bar(id(<!UNRESOLVED_REFERENCE!>Z<!>))
    bar(X)

    barTwo(X, <!UNRESOLVED_REFERENCE!>Z<!>)
    barTwo(<!UNRESOLVED_REFERENCE!>Z<!>, <!UNRESOLVED_REFERENCE!>Z<!>)

    val a = bar(<!UNRESOLVED_REFERENCE!>Z<!>)
    val b: Unit = bar(<!UNRESOLVED_REFERENCE!>Z<!>)
}

/* GENERATED_FIR_TAGS: enumDeclaration, enumEntry, functionDeclaration, localProperty, nullableType, propertyDeclaration,
typeParameter */
