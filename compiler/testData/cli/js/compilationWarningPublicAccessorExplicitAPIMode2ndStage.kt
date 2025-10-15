public class Outer {
    public fun foo(): String = "foo"
    public inner class Inner {
        @Suppress("NOTHING_TO_INLINE")
        public inline fun inlineFoo(): String = foo()
    }
}
