// WITH_RUNTIME
data class A(var x: Int)

fun convert(f: (A) -> Unit) {}

fun test() {
    convert <caret>{
        val x = it.x

        run {
            val x = 1
            val z = it.x
        }
    }
}