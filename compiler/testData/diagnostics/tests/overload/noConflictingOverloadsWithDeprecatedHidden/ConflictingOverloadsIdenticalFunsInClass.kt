class A() {
    <!CONFLICTING_OVERLOADS!>fun b()<!> {
    }

    <!CONFLICTING_OVERLOADS!>@Deprecated("a", level = DeprecationLevel.HIDDEN) fun b()<!> {
    }
}

open class B() {
    <!CONFLICTING_OVERLOADS!>open fun b()<!> {
    }

    <!CONFLICTING_OVERLOADS!>@Deprecated("a", level = DeprecationLevel.HIDDEN) fun b()<!> {
    }
}

open class C() {
    <!CONFLICTING_OVERLOADS!>fun b()<!> {
    }

    <!CONFLICTING_OVERLOADS!>@Deprecated("a", level = DeprecationLevel.HIDDEN) open fun b()<!> {
    }
}
