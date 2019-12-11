// !DIAGNOSTICS: -UNUSED_PARAMETER -NOT_YET_SUPPORTED_IN_INLINE

inline fun<reified T> foo(x: T) {
    fun<reified R> bar() {

    }

    bar<T>()
}
