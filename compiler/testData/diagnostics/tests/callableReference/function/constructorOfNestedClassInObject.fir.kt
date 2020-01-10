// KT-15951 Callable reference to class constructor from object is not resolved

object A {
    class Wrapper
}

class Outer {
    companion object {
        class Wrapper
    }
}

fun test() {
    A::Wrapper
    (A)::Wrapper

    Outer.Companion::Wrapper
    (Outer.Companion)::Wrapper
    <!UNRESOLVED_REFERENCE!>Outer::Wrapper<!>
    <!UNRESOLVED_REFERENCE!>(Outer)::Wrapper<!>
}
