class A() {
    <!CONFLICTING_OVERLOADS!>fun b()<!> {
    }

    <!CONFLICTING_OVERLOADS!>@Deprecated("b", level = DeprecationLevel.HIDDEN)
    fun b()<!> {
    }
}
