// "Remove parameter 'i'" "true"
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
    z.f(<caret>)
}