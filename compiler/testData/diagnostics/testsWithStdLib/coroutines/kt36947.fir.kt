val foo = iterator {
    yield(0)
    val nullable: String? = null
    nullable.<!INAPPLICABLE_CANDIDATE!>length<!>
    nullable.<!INAPPLICABLE_CANDIDATE!>get<!>(2)
}