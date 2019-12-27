// !LANGUAGE: +InlineClasses

inline class R(private val r: Int) {
    fun test() = pv

    companion object {
        private val pv = "OK"
    }
}

fun box() = R(0).test()