// !DIAGNOSTICS: -UNUSED_PARAMETER

data class C(val c: Int) {
    fun `copy$default`(c: C, x: Int, m: Int, mh: Any) = C(this.c)
}