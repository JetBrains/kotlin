interface Test<in I, out O> {
    val internal_val: <!TYPE_VARIANCE_CONFLICT!>I<!>
    public val public_val: <!TYPE_VARIANCE_CONFLICT!>I<!>
    <!WRONG_MODIFIER_CONTAINING_DECLARATION!>protected<!> val protected_val: <!TYPE_VARIANCE_CONFLICT!>I<!>
    <!PRIVATE_PROPERTY_IN_INTERFACE!>private<!> val private_val: I

    var interlan_private_set: <!TYPE_VARIANCE_CONFLICT!>O<!>
        <!PRIVATE_SETTER_FOR_ABSTRACT_PROPERTY!>private<!> set
    public var public_private_set: <!TYPE_VARIANCE_CONFLICT!>O<!>
        <!PRIVATE_SETTER_FOR_ABSTRACT_PROPERTY!>private<!> set
    <!WRONG_MODIFIER_CONTAINING_DECLARATION!>protected<!> var protected_private_set: <!TYPE_VARIANCE_CONFLICT!>O<!>
        <!PRIVATE_SETTER_FOR_ABSTRACT_PROPERTY!>private<!> set
    <!PRIVATE_PROPERTY_IN_INTERFACE!>private<!> var private_private_set: O
        private set

    fun internal_fun(i: <!TYPE_VARIANCE_CONFLICT!>O<!>) : <!TYPE_VARIANCE_CONFLICT!>I<!>
    public fun public_fun(i: <!TYPE_VARIANCE_CONFLICT!>O<!>) : <!TYPE_VARIANCE_CONFLICT!>I<!>
    <!WRONG_MODIFIER_CONTAINING_DECLARATION!>protected<!> fun protected_fun(i: <!TYPE_VARIANCE_CONFLICT!>O<!>) : <!TYPE_VARIANCE_CONFLICT!>I<!>
    <!PRIVATE_FUNCTION_WITH_NO_BODY!>private<!> fun private_fun(i: O) : I
}