// FIR_IDENTICAL
typealias TA = Sealed

sealed class Sealed {
    object First: Sealed()
    open class NonFirst: Sealed() {
        object Second: NonFirst()
        object Third: NonFirst()
        fun foo(): Int {
            val s = object: <!SEALED_SUPERTYPE_IN_LOCAL_CLASS!>Sealed<!>() {}
            val s2 = object: <!SEALED_SUPERTYPE_IN_LOCAL_CLASS!>TA<!>() {}
            class Local: <!SEALED_SUPERTYPE_IN_LOCAL_CLASS!>Sealed<!>() {}
            return s.hashCode()
        }
    }
    val p: Sealed = object: <!SEALED_SUPERTYPE_IN_LOCAL_CLASS!>Sealed<!>() {}
}
