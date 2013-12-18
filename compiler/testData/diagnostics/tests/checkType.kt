// !CHECK_TYPE

trait A
trait B : A
trait C : B

fun test(b: B) {
    b checkType { it : _<B> }
    b checkType { <!TYPE_MISMATCH!>it<!> : _<A> }
    b checkType { <!TYPE_MISMATCH!>it<!> : _<C> }
}
