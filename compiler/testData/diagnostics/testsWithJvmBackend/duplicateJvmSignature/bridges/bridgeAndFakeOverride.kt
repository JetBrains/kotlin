// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER
// ISSUE: KT-13712

interface Restrict

object EmptyRestrict : Restrict

interface RestrictedGeneric<T: Restrict>: Restrict {
    fun accept(obj: T): Int
    fun acceptOpen(obj: T)
}

open class Foo {
    fun accept(obj: Restrict): Int = 0
    open fun acceptOpen(obj: Restrict) {
    }
}

<!CONFLICTING_INHERITED_JVM_DECLARATIONS, CONFLICTING_INHERITED_JVM_DECLARATIONS!>class Bar : Foo(), RestrictedGeneric<Bar>{
    override fun accept(obj: Bar): Int = 0
    override fun acceptOpen(obj: Bar) {
    }
}<!>
