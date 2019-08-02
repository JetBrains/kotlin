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
            materialize() // Should be an errror
        }
    )
}