// FIR_IDENTICAL
package test

class InnerTypes<E, F> {
    inner class Inner<G, H> {
        inner class Inner3<I> {
            fun foo(
                    x: InnerTypes<String, F>.Inner<G, Int>,
                    y: Inner<E, Double>,
                    z: InnerTypes<String, F>.Inner<G, Int>.Inner3<Double>,
                    w: Inner3<*>) {}
        }
    }

    inner class Inner2

    fun bar(x: InnerTypes<String, Double>.Inner2, y: Inner2) {}
}
