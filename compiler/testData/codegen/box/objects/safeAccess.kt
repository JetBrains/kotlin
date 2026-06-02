// KT-5159

object Test {
    val a = "OK"
}

fun test(): String? = Test?.a

fun box(): String = test()!!
