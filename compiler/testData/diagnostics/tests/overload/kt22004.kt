// ISSUE: KT-22004

class A() {
    <!CONFLICTING_OVERLOADS!>fun b()<!> {
    }

    <!CONFLICTING_OVERLOADS!>@Deprecated("test", level = DeprecationLevel.HIDDEN)
    fun b()<!> {
    }
}
