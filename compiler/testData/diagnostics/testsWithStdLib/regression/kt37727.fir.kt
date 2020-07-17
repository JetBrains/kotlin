data class A(val x: Set<CLassNotFound> = setOf()) {
    fun with(x: Set<CLassNotFound>? = null) {
        A(x ?: this.x)
    }
}
