data class A(val x: Set<<!UNRESOLVED_REFERENCE!>CLassNotFound<!>> = setOf()) {
    fun with(x: Set<<!UNRESOLVED_REFERENCE!>CLassNotFound<!>>? = null) {
        A(<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>x<!> ?: this.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>x<!>)
    }
}
