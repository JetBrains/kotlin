// ISSUE: KT-59649
<!PROPERTY_WITH_NO_TYPE_NO_INITIALIZER!>val prop<!>
    get() {
        fun smth(s: String) = 1
        return smth("awd")
    }
