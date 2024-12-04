// https://youtrack.jetbrains.com/issue/KT-50289/EXCBADACCESS-getting-non-null-property-in-safe-call-chain
// IGNORE_NATIVE: optimizationMode=DEBUG
// IGNORE_NATIVE: optimizationMode=NO

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
