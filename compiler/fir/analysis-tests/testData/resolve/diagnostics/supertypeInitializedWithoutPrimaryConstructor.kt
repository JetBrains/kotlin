class A
class B : A

class C(x: Int)
<!INAPPLICABLE_CANDIDATE!>class D : C<!>
class E : C(10)
class F() : C(10)

<!SUPERTYPE_INITIALIZED_WITHOUT_PRIMARY_CONSTRUCTOR!>class G : C(10) {
    constructor() : super(1)
}<!>