// See KT-13401
interface Rec<T: Rec<T>> {
    fun t(): T
}
interface Super<U> {
    fun foo(p: Rec<*>) = p.t()
}