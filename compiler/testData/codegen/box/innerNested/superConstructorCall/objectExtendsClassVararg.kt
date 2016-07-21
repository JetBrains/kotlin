open class SomeClass(val some: Double, val other: Int, vararg val args: String) {
    fun result() = args[1]
}

fun box(): String {
    return object : SomeClass(3.14, 42, "No", "OK", "Yes") {
    }.result()
}
