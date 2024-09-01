class A {
    fun a(a: Int): Int = 0

    @Deprecated("a", level = DeprecationLevel.HIDDEN) fun a(a: Int) {
    }
}

open class B {
    open <!CONFLICTING_OVERLOADS!>fun a(a: Int): Int<!> = 0

    @Deprecated("a", level = DeprecationLevel.HIDDEN) <!CONFLICTING_OVERLOADS!>fun a(a: Int)<!> {
    }
}

open class C {
    <!CONFLICTING_OVERLOADS!>fun a(a: Int): Int<!> = 0

    @Deprecated("a", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun a(a: Int)<!> {
    }
}
