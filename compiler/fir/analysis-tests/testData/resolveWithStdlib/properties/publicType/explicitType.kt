class Something {
    val junk1: String = "some junk"
        get

    <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>val junk2: String<!>
        get(): <!REDUNDANT_GETTER_TYPE_CHANGE!>Any?<!>

    val junk25: String
        get(): <!REDUNDANT_GETTER_TYPE_CHANGE!>Any?<!> = "Test"

    val junk3: String = "some junk"
        get(): <!WRONG_GETTER_RETURN_TYPE!>Int<!> = field

    val junk4: String = "some junk"
        get(): String = field

    protected val junk5: Any = "some junk"
        <!GETTER_VISIBILITY_LESS_OR_INCONSISTENT_WITH_PROPERTY_VISIBILITY!>private<!> get(): <!WRONG_GETTER_RETURN_TYPE!>String<!>

    protected val junk6: Any = "some junk"
        <!GETTER_VISIBILITY_LESS_OR_INCONSISTENT_WITH_PROPERTY_VISIBILITY!>private<!> get(): Any

    protected val junk7: String = "some junk"
        <!GETTER_VISIBILITY_LESS_OR_INCONSISTENT_WITH_PROPERTY_VISIBILITY!>private<!> get(): Any? {
            return field
        }

    open class A
    class B : A()

    class OutTester<out T>
    class InTester<in T>

    private val junkOutBA: OutTester<A> = OutTester()
        public get(): <!WRONG_GETTER_RETURN_TYPE!>OutTester<B><!>

    private val junkOutAB: OutTester<B> = OutTester()
        public get(): OutTester<A>

    private val junkInBA: InTester<A> = InTester()
        public get(): InTester<B>

    private val junkInAB: InTester<B> = InTester()
        public get(): <!WRONG_GETTER_RETURN_TYPE!>InTester<A><!>
}
