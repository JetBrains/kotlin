class Wrapper {
    private val dummy = object : Bar {}
    private val bar = object : Bar by dummy {}
}

interface Bar {
    val foo: String
        get() = ""
}
