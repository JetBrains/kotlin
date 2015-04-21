// !CHECK_TYPE

trait A
trait B : A
trait C : B

fun test(b: B) {
    b checkType { _<B>() }
    b checkType { <!TYPE_MISMATCH!>_<!><A>() }
    b checkType { <!TYPE_MISMATCH!>_<!><C>() }
}
