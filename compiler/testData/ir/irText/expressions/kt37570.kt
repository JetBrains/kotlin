// IGNORE_BACKEND: JKLIB
fun a() = "string"

class A {
    val b: String
    init {
        a().apply {
            b = this
        }
    }
}
