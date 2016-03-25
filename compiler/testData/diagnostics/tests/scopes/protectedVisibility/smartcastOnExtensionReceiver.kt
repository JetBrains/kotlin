abstract class A<T : Any> {
    abstract protected fun T.foo()

    fun bar(x: T?) {
        if (x != null) {
            <!DEBUG_INFO_SMARTCAST!>x<!>.foo()
        }
    }
}
