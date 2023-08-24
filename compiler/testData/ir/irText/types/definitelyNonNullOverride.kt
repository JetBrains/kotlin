// FIR_IDENTICAL
//!LANGUAGE: +DefinitelyNonNullableTypes
// SKIP_KT_DUMP

open class B<T> {
    open fun foo(t: T) {}
    open fun bar(t: T) {}
    open fun qux(b: B<T>) {}
    open fun <F> six(t: T, q: F) {}
}

class D<T> : B<T & Any>() {
    override fun foo(t: T & Any) {}
}


// KT-49420
//interface I1<T> {
//    fun foo(t: T)
//    fun bar(t: T)
//}
//
//interface I2<T> {
//    fun foo(t: T)
//    fun bar(t: T)
//}
//
//
//interface II<T> : I1<T>, I2<T & Any> {
//    override fun foo(t: T & Any)
//}
//
//abstract class Q : II<String?>