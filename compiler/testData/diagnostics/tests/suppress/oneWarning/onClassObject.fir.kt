class C {
    @Suppress("REDUNDANT_NULLABLE")
    companion object {
        val foo: String?? = null as Nothing??
    }
}