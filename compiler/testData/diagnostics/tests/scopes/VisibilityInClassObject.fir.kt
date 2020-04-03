fun devNull(obj: Any?) {}

open class A {
    companion object {
        val internal_val = 1
        public val public_val: Int = 2
        private val private_val = 3
        protected val protected_val: Int = 5
    }

    fun fromClass() {
        devNull(internal_val)
        devNull(public_val)
        devNull(private_val)
        devNull(protected_val)
    }
}

fun fromOutside() {
    devNull(A.internal_val)
    devNull(A.public_val)
    devNull(A.<!INAPPLICABLE_CANDIDATE!>private_val<!>)
    devNull(A.<!INAPPLICABLE_CANDIDATE!>protected_val<!>)
}

class B: A() {
    fun fromSubclass() {
        devNull(A.internal_val)
        devNull(A.public_val)
        devNull(A.<!INAPPLICABLE_CANDIDATE!>private_val<!>)
        devNull(A.protected_val)
    }
}
