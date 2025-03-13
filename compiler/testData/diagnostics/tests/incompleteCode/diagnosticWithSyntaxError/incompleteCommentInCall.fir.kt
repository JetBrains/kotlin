// RUN_PIPELINE_TILL: FRONTEND

class Box(val value: String)

fun main() {
    val box = Box("")
    O.foo(
        aaaa = box.value,
        /
    bbbb = false
    )
}

object O {
    fun foo(aa: String, aaaa: String, bbbb: Boolean) {}
}