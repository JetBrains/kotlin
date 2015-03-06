class C {
    suppress("warnings")
    default object {
        val foo: String?? = null as Nothing?
    }
}