class A {
    <!CONFLICTING_OVERLOADS!>fun a(a: Int): Int<!> = 0

    <!CONFLICTING_OVERLOADS!>@Deprecated("a", level = DeprecationLevel.HIDDEN)
    fun a(a: Int)<!> {
    }
}
