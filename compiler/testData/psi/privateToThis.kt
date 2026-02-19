package p

class PrivateToThis<in I> {
    private val foo: I = null!!
    private var bar: I = null!!

    private val val_with_accessors: I
        get() = null!!

    private var var_with_accessors: I
        get() = null!!
        set(value: I) {}

    private fun bas(): I = null!!
}