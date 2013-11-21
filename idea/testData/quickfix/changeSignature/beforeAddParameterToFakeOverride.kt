// "Add parameter to function 'f'" "true"
trait Z {
    fun f()
}

trait ZZ {
    fun f()
}

trait ZZZ: Z, ZZ {
}

trait ZZZZ : ZZZ {
    override fun f()
}

fun usage(z: ZZZ) {
    z.f(<caret>3)
}