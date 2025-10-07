// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER
// ISSUE: KT-13712

interface Restrict

object EmptyRestrict : Restrict

interface RestrictedGeneric<T: Restrict>: Restrict {
    fun accept(obj: T): Int
}

open class Foo {
    fun accept(obj: Restrict): Int = 0
}

<!CONFLICTING_INHERITED_JVM_DECLARATIONS!>class Bar<T> : Foo(), RestrictedGeneric<Bar<T>>{
    override fun accept(obj: Bar<T>): Int = 0
}<!>
