// !LANGUAGE: +InlineClasses

inline class R(private val r: Int) {
    fun test() =
        object {
            override fun toString() = "OK"
        }.toString()
}

fun box() = R(0).test()