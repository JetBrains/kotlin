// FIR_IDENTICAL
class C1<T1>

class C2<T3> {
    fun <T2> test() {
        class LocalClass
        C1<LocalClass>()
    }
}

class A<T> {
    private inner class Inner
    private val test = ArrayList<Inner>()
}
