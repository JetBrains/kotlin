package test

class InnerClasses<E, F> {
    inner class Inner<G, H> {
        inner class Inner3<I> {
            fun foo(
                    x: InnerClasses<String, F>.Inner<G, Int>,
                    y: Inner<E, Double>,
                    z: InnerClasses<String, F>.Inner<G, Int>.Inner3<Double>,
                    w: Inner3<*>) {}
        }
    }

    inner class Inner2

    fun bar(x: InnerClasses<String, Double>.Inner2, y: Inner2) {}
}
