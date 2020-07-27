data class A(val x: <!OTHER_ERROR, OTHER_ERROR, OTHER_ERROR, OTHER_ERROR, OTHER_ERROR!>Set<CLassNotFound><!> = setOf()) {
    fun with(x: <!OTHER_ERROR, OTHER_ERROR!>Set<CLassNotFound>?<!> = null) {
        A(x ?: this.x)
    }
}
