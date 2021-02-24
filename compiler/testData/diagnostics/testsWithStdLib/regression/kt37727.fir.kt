data class A(val x: Set<<!UNRESOLVED_REFERENCE!>CLassNotFound<!>> = setOf()) {
    fun with(x: Set<<!UNRESOLVED_REFERENCE!>CLassNotFound<!>>? = null) {
        A(x ?: this.x)
    }
}
