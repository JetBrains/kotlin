package foo

fun foo() {
    class LocalClass<A> {
        inner class LocalInner1<B, C> {
            inner class LocalI<caret>nner2<D, E, F>
        }
    }
}