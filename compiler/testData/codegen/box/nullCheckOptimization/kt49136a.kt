// IGNORE_BACKEND: NATIVE

abstract class Z {
    init {
        check(this)
    }

    abstract val b: B
}

class A(override val b: B) : Z()

class B(val c: String)

fun use(a: Any?) {}

fun check(z: Z) {
    use(z?.b?.c)
}

fun box(): String {
    A(B(""))
    return "OK"
}
