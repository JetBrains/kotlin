interface A {
    fun foo(): String
}

enum class Zzz(val zzz: String, val x: Int) : A {
    Z1("z1", 1),
    Z2("z2", 2),
    Z3("z3", 3);

    override fun foo(): String{
        return "('$zzz', $x)"
    }
}

fun main(args: Array<String>) {
    println(Zzz.Z3.foo())
    val a: A = Zzz.Z3
    println(a.foo())
}