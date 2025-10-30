// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL

class AList<T>() : List<T> by <!UNRESOLVED_REFERENCE!>inner<!> {
    private val inner = ArrayList<T>()
}

open class X(bar: Int)

class Y : X(<!UNRESOLVED_REFERENCE!>bar<!>) {
    val bar = 4
}

/* GENERATED_FIR_TAGS: classDeclaration, inheritanceDelegation, integerLiteral, nullableType, primaryConstructor,
propertyDeclaration, typeParameter */
