fun <T> materialize(): T = throw Exception()

interface A

fun takeA(a: A) {}

fun test() {
    takeA(
        try {
            materialize()
        } catch (e: Exception) {
            materialize()
        } finally {
            <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>materialize<!>() // Should be an errror
        }
    )
}
