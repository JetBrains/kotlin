data class A(val x: <!UNRESOLVED_REFERENCE!>Set<CLassNotFound><!> = setOf()) {
    fun with(x: <!UNRESOLVED_REFERENCE!>Set<CLassNotFound>?<!> = null) {
        A(x ?: this.x)
    }
}
