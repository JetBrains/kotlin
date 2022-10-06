// SKIP_TXT
interface Comp<T> {
    fun foo(t: T)
}

fun <E : Any> foo(c: Comp<in E>, e: E?) {
    if (e == null) return
    c.foo(<!DEBUG_INFO_SMARTCAST!>e<!>)
}
