class A() {
    fun b() {
    }

    @Deprecated("a", level = DeprecationLevel.HIDDEN) fun b() {
    }
}

open class B() {
    open <!CONFLICTING_OVERLOADS!>fun b()<!> {
    }

    @Deprecated("a", level = DeprecationLevel.HIDDEN) <!CONFLICTING_OVERLOADS!>fun b()<!> {
    }
}

open class C() {
    <!CONFLICTING_OVERLOADS!>fun b()<!> {
    }

    @Deprecated("a", level = DeprecationLevel.HIDDEN) open <!CONFLICTING_OVERLOADS!>fun b()<!> {
    }
}
