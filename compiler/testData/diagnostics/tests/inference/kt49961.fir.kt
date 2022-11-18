class Xyz {
    fun x(): String? {
        return try {
            <!UNSUPPORTED!>[<!UNRESOLVED_REFERENCE!>a<!>]<!> <!USELESS_ELVIS!>?: <!ARGUMENT_TYPE_MISMATCH, UNRESOLVED_REFERENCE!>XYZ<!><!>
        }
        catch (e: Exception) {
            null
        }
    }
}
