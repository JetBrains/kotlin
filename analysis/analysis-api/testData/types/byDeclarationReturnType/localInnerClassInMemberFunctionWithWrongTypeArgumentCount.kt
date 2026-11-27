// COMPILATION_ERRORS

class Outer<S> {
    fun <T> test() {
        class Local<A> {
            inner class Inner<B>
        }

        fun <R> fo<caret>o(): Local<String>.Inner<Int, Long>? = null
    }
}
