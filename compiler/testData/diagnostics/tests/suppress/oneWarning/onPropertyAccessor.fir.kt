class C {
    val foo: String?
        @Suppress("REDUNDANT_NULLABLE")
        get(): String?? = null as Nothing??
}