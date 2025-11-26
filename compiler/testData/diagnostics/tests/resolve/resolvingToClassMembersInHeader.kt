// RUN_PIPELINE_TILL: FRONTEND

class AList<T>() : List<T> by <!UNRESOLVED_REFERENCE!>inner<!> {
    private val inner = ArrayList<T>()
}

open class X(bar: Int)

class Y : X(<!UNRESOLVED_REFERENCE!>bar<!>) {
    val bar = 4
}
class Y2 : X(<!NO_THIS!>this<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>bar<!>) {
    val bar = 4
}
class Y3 : X(this<!UNRESOLVED_REFERENCE!>@Y3<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>bar<!>) {
    val bar = 4
}

/* GENERATED_FIR_TAGS: classDeclaration, inheritanceDelegation, integerLiteral, nullableType, primaryConstructor,
propertyDeclaration, typeParameter */
