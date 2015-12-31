public interface Rec<T : Rec<T>> {
    public fun foo(): T?
}

fun acceptRec(<!UNUSED_PARAMETER!>rec<!>: Rec<*>) {
}

fun checkRec(rec: Rec<*>) {
    acceptRec(<!TYPE_MISMATCH!>rec.foo()<!>)
}