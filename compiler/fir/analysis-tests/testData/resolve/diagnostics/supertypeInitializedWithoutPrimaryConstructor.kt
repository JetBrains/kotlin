class A
class B : <!SUPERTYPE_NOT_INITIALIZED!>A<!>

class C(x: Int)
<!INAPPLICABLE_CANDIDATE!>class D : <!SUPERTYPE_NOT_INITIALIZED!>C<!><!>
class E : C(10)
class F() : C(10)

<!SUPERTYPE_INITIALIZED_WITHOUT_PRIMARY_CONSTRUCTOR!>class G : C(10) {
    constructor() : super(1)
}<!>
