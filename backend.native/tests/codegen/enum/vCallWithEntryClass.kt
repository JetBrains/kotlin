enum class Zzz {
    Z1 {
        override fun f() = "z1"
    },

    Z2 {
        override fun f() = "z2"
    };

    open fun f() = ""
}

fun main(args: Array<String>) {
    println(Zzz.Z1.f() + Zzz.Z2.f())
}