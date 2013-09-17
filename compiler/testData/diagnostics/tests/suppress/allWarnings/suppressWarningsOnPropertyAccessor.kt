class C {
    val foo: String?
        [suppress("warnings")]
        get(): String?? = null as Nothing?
}