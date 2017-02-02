enum class Zzz(val zzz: String, val x: Int) {
    Z1("z1", 1),
    Z2("z2", 2)
}

fun main(args: Array<String>) {
    println(Zzz.Z1.zzz + Zzz.Z2.x)
}