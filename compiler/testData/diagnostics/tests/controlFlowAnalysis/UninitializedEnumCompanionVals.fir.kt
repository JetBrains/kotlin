// SKIP_TXT

enum class A(val z: Any) {
    Y(<!UNINITIALIZED_ENUM_COMPANION, UNINITIALIZED_VARIABLE!>x<!>);

    companion object {
        val x = A.Y.ordinal
    }
}

enum class B(val z: Any) {
    Y(<!UNINITIALIZED_VARIABLE!><!UNINITIALIZED_ENUM_COMPANION!>B<!>.x<!>);

    companion object {
        val x = B.Y.ordinal
    }
}
