internal class Method<out T : String?>private constructor(val name: String, val signature: T) {
    companion object {
        operator fun invoke(name: String): Method<String?> = TODO()
    }
}

fun foo() {
    Method("asd")
}
