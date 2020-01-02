class C {
    fun foo(): Any? {
        @Suppress("REDUNDANT_NULLABLE")
        val v: String?? = null as Nothing??
        return v
    }
}