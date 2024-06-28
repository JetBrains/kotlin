fun <T> magic(): T = null!!

class Q {
    fun <E, F> f<caret>oo() = {
        class C<G> {
            val e: E = magic()
            val f: F = magic()
            val g: G = magic()
        }
        C<F>()
    }
}