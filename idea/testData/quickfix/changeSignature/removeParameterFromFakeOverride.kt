// "Remove parameter 'i'" "true"
interface Z {
    fun f(i: Int)
}

interface ZZ {
    fun f(i: Int)
}

interface ZZZ: Z, ZZ {
}

interface ZZZZ : ZZZ {
    override fun f(i: Int)
}

fun usage(z: ZZZ) {
    z.f(<caret>)
}