interface Test<in I, out O> {
    val internal_val: <!TYPE_VARIANCE_CONFLICT!>I<!>
    public val public_val: <!TYPE_VARIANCE_CONFLICT!>I<!>
    <!DEPRECATED_MODIFIER_CONTAINING_DECLARATION!>protected<!> val protected_val: <!TYPE_VARIANCE_CONFLICT!>I<!>
    <!DEPRECATED_MODIFIER_CONTAINING_DECLARATION!>private<!> val private_val: I

    var interlan_private_set: <!TYPE_VARIANCE_CONFLICT!>O<!>
        <!DEPRECATED_MODIFIER_CONTAINING_DECLARATION!>private<!> set
    public var public_private_set: <!TYPE_VARIANCE_CONFLICT!>O<!>
        <!DEPRECATED_MODIFIER_CONTAINING_DECLARATION!>private<!> set
    <!DEPRECATED_MODIFIER_CONTAINING_DECLARATION!>protected<!> var protected_private_set: <!TYPE_VARIANCE_CONFLICT!>O<!>
        <!DEPRECATED_MODIFIER_CONTAINING_DECLARATION!>private<!> set
    <!DEPRECATED_MODIFIER_CONTAINING_DECLARATION!>private<!> var private_private_set: O
        <!DEPRECATED_MODIFIER_CONTAINING_DECLARATION!>private<!> set

    fun internal_fun(i: <!TYPE_VARIANCE_CONFLICT!>O<!>) : <!TYPE_VARIANCE_CONFLICT!>I<!>
    public fun public_fun(i: <!TYPE_VARIANCE_CONFLICT!>O<!>) : <!TYPE_VARIANCE_CONFLICT!>I<!>
    <!DEPRECATED_MODIFIER_CONTAINING_DECLARATION!>protected<!> fun protected_fun(i: <!TYPE_VARIANCE_CONFLICT!>O<!>) : <!TYPE_VARIANCE_CONFLICT!>I<!>
    <!DEPRECATED_MODIFIER_CONTAINING_DECLARATION!>private<!> fun private_fun(i: O) : I
}