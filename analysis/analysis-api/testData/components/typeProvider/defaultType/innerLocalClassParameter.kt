package foo

fun foo() {
    class LocalClass<A> {
        inner class LocalInner1<B, C> {
            inner class LocalInner2<D, E<caret>e, F>
        }
    }
}