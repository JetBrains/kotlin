fun <T> getT(): T = null!!

class A<in I>(init: I) {
    private val i: I

    init {
        i = getT()
    }

    private var i2 = i
    private val i3: I

    private var i4 = getT<I>()

    init {
        i2 = getT()
        i3 = init
        i4 = i3
    }
}