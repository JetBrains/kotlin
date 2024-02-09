data class A(val x: Set<<!UNRESOLVED_REFERENCE!>CLassNotFound<!>> = <!CANNOT_INFER_PARAMETER_TYPE!>setOf<!>()) {
    fun with(x: Set<<!UNRESOLVED_REFERENCE!>CLassNotFound<!>>? = null) {
        A(x ?: this.x)
    }
}
