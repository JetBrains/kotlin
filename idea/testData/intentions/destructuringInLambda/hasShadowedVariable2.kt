// WITH_RUNTIME
data class A(var x: Int)

fun convert(f: (A) -> Unit) {}

fun test() {
    convert { <caret>a ->
        val x = a.x

        run {
            val x = 1
            val z = a.x
        }
    }
}