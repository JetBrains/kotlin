open class A
class B : <!SUPERTYPE_NOT_INITIALIZED!>A<!>

open class C(x: Int)
class D : <!NO_VALUE_FOR_PARAMETER, SUPERTYPE_NOT_INITIALIZED!>C<!>
class E : C(10)
class F() : C(10)

<!SUPERTYPE_INITIALIZED_WITHOUT_PRIMARY_CONSTRUCTOR!>class G : C(10) {
    constructor() : super(1)
}<!>
