class C {
    @Suppress("REDUNDANT_NULLABLE", "UNNECESSARY_NOT_NULL_ASSERTION")
    companion object {
        val foo: String?? = ""!! as String??
    }
}