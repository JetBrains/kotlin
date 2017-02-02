enum class Zzz(val zzz: String, val x: Int) {
    Z1("z1", 1),
    Z2("z2", 2),
    Z3("z3", 3);

    override fun toString(): String{
        return "('$zzz', $x)"
    }
}

fun main(args: Array<String>) {
    println(Zzz.Z3.toString())
}