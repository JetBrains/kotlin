// KT-5159

object Test {
    val a = "OK"
}

fun box(): String = Test?.a
