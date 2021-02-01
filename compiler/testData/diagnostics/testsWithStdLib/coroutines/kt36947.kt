// FIR_IDENTICAL
val foo = iterator {
    yield(0)
    val nullable: String? = null
    nullable<!UNSAFE_CALL!>.<!>length
    nullable<!UNSAFE_CALL!>.<!>get(2)
}