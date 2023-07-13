open class A(i: Int)

class B : <!SUPERTYPE_INITIALIZED_WITHOUT_PRIMARY_CONSTRUCTOR!>A(<!DEBUG_INFO_MISSING_UNRESOLVED!>x<!>)<!> {
    constructor(i: Int) : super(i)
}
