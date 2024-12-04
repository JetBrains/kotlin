// FIR_IDENTICAL

fun a() = "string"

class A {
    val b: String
    init {
        a().apply {
            b = this
        }
    }
}
