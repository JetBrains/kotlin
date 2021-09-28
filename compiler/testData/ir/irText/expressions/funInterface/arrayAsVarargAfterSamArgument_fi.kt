// FIR_IDENTICAL

fun interface IRunnable {
    fun run()
}

fun foo1(r: IRunnable, vararg s: String) {}
fun foo2(r1: IRunnable, r2: IRunnable, vararg s: String) {}

fun test(fn: () -> Unit, r: IRunnable, s: String, arr: Array<String>) {
    foo1({}, s)
    foo1({}, *arr)
    foo1(fn, s)
    foo1(fn, *arr)
    foo1(r, s)
    foo1(r, *arr)

    foo2({}, {}, s)
    foo2({}, {}, *arr)
    foo2(fn, {}, s)
    foo2(fn, {}, *arr)
    foo2(r, {}, s)
    foo2(r, {}, *arr)
}
