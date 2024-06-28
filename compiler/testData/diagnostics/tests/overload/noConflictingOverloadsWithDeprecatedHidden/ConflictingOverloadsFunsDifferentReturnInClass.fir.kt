class A {
    fun a(a: Int): Int = 0

    @Deprecated("a", level = DeprecationLevel.HIDDEN) fun a(a: Int) {
    }
}

open class B {
    <!CONFLICTING_OVERLOADS!>open fun a(a: Int): Int<!> = 0

    <!CONFLICTING_OVERLOADS!>@Deprecated("a", level = DeprecationLevel.HIDDEN) fun a(a: Int)<!> {
    }
}

open class C {
    <!CONFLICTING_OVERLOADS!>fun a(a: Int): Int<!> = 0

    <!CONFLICTING_OVERLOADS!>@Deprecated("a", level = DeprecationLevel.HIDDEN) open fun a(a: Int)<!> {
    }
}
