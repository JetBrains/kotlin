// FIR_DISABLE_LAZY_RESOLVE_CHECKS
// FIR_IDENTICAL

interface Aaa<T> {
  fun zzz(value: T): Unit
}

class Bbb<T>() : Aaa<T> {
    override fun zzz(value: T) { }
}

fun foo() {
    var a = Bbb<Double>()
    a.zzz(10.0)
}
