// "Add parameter to function 'f'" "true"
trait Z {
    fun f(i: Int)
}

trait ZZ {
    fun f(i: Int)
}

trait ZZZ: Z, ZZ {
}

trait ZZZZ : ZZZ {
    override fun f(i: Int)
}

fun usage(z: ZZZ) {
    z.f(3)
}