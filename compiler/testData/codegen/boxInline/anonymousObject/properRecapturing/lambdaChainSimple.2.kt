package test

inline fun <T> inlineFun(arg: T, crossinline f: (T) -> Unit) {
    {
        f(arg)
    }()
}
