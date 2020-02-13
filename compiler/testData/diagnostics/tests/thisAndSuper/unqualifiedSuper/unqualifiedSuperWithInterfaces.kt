//  Interface  AnotherInterface
//          \  /
//           \/
//     DerivedInterface
//

interface Interface {
    fun foo() {}
    fun ambiguous() {}
    val ambiguousProp: Int
        get() = 222
}

interface AnotherInterface {
    fun ambiguous() {}
    val ambiguousProp: Int
        get() = 333
}

interface DerivedInterface: Interface, AnotherInterface {
    override fun foo() { super.foo() }
    override fun ambiguous() {
        <!AMBIGUOUS_SUPER!>super<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>ambiguous<!>()
    }
    override val ambiguousProp: Int
        get() = <!AMBIGUOUS_SUPER!>super<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>ambiguousProp<!>
}

