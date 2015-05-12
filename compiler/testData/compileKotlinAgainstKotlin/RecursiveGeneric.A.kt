package a

interface Rec<R, out T: Rec<R, T>> {
    fun t(): T
}

interface Super {
    fun foo(p: Rec<*, *>) = p.t()
}