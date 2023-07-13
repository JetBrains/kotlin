open class A(i: Int)

<!SUPERTYPE_INITIALIZED_WITHOUT_PRIMARY_CONSTRUCTOR!>class B : A(<!UNRESOLVED_REFERENCE!>x<!>) {
    constructor(i: Int) : super(i)
}<!>
