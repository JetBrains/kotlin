class A<AA> {
    inner class B<BB> {
        typealias TA<X> = List<X>

        fun f<caret>oo(): TA<Int>? = null
    }
}
