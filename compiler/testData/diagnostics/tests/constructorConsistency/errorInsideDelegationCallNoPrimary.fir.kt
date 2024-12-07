// RUN_PIPELINE_TILL: FRONTEND
open class A(i: Int)

class B : <!SUPERTYPE_INITIALIZED_WITHOUT_PRIMARY_CONSTRUCTOR!>A<!>(<!UNRESOLVED_REFERENCE!>x<!>) {
    constructor(i: Int) : super(i)
}
