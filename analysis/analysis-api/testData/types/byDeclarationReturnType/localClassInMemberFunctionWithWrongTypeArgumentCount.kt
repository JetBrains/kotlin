// COMPILATION_ERRORS

class Outer<S> {
    fun <F> test() {
        class Local<T>

        fun fo<caret>o(): Local? = null
    }
}
