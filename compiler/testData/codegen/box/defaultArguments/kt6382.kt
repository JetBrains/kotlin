// WITH_STDLIB

fun box(): String {
    return if (A().run() == "Aabc") "OK" else "fail"
}

public class A {
    fun run() =
            with ("abc") {
                show()
            }

    private fun String.show(p: Boolean = false): String = getName() + this

    private fun getName() = "A"
}
