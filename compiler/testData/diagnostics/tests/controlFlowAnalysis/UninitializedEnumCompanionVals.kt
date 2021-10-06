// SKIP_TXT

enum class A(val z: Any) {
    Y(<!UNINITIALIZED_VARIABLE!>x<!>);

    companion object {
        val x = A.Y.ordinal
    }
}

enum class B(val z: Any) {
    Y(B.x);

    companion object {
        val x = B.Y.ordinal
    }
}
