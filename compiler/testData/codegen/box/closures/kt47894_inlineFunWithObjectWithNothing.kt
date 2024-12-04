interface Kla6 {
    fun fu32()
}

class Kla7 {
    inline fun fu33(crossinline f: (Int) -> Any) =
        object : Kla6 {
            override fun fu32() {
                f(1)
            }
            var ttmh: Int = throw RuntimeException()
        }.fu32()

    inline fun fu34(crossinline f: (Int) -> Any) =
        fu33 { f(1) }
}

fun box(): String = try {
    Kla7().fu34 {}
    "Fail"
} catch (e: RuntimeException) {
    "OK"
}
