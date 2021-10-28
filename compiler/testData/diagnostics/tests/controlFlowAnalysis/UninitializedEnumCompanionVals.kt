// SKIP_TXT

enum class A(val z: Any) {
    Y(<!UNINITIALIZED_ENUM_COMPANION_WARNING, UNINITIALIZED_VARIABLE!>x<!>);

    companion object {
        val x = A.Y.ordinal
    }
}

enum class B(val z: Any) {
    Y(<!UNINITIALIZED_ENUM_COMPANION_WARNING!>B<!>.x);

    companion object {
        val x = B.Y.ordinal
    }
}
