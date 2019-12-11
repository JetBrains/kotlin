class C {
    val foo: String?
        @Suppress("REDUNDANT_NULLABLE", "UNNECESSARY_NOT_NULL_ASSERTION")
        get(): String?? = ""!! as String??
}