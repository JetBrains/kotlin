package foo

actual class C {
    fun zzz() {
    }
}

fun works() {
    C().zzz()
}

fun doesnNot() {
    getC()?.zzz()
    getC()?.zzz()
}