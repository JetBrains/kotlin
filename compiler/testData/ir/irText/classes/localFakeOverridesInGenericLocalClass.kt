// DUMP_IR_AFTER_INLINE

open class Base<T> {
    fun foo(): T? = null
}

// A call to a fake override of a generic local class: the type arguments of the receiver type must be used to compute
// the type of the dispatch receiver of the non-local member, as the type parameters of the local class are out of scope
// at the call site.
fun test(): Int? {
    class Local<T> : Base<T>()
    return Local<Int>().foo()
}
