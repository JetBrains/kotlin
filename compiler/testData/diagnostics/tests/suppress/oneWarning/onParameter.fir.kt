class C {
    fun foo(@Suppress("REDUNDANT_NULLABLE") p: String?? = null as Nothing??) = p
}