// !LANGUAGE: +InlineClasses

fun <T> eval(fn: () -> T) = fn()

inline class R(private val r: Int) {
    fun test() = eval { "OK" }
}

fun box() = R(0).test()