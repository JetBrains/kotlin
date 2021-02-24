// !LANGUAGE: +InlineClasses

inline class R(private val r: Int) {
    fun test() = { "OK" }()
}

fun box() = R(0).test()