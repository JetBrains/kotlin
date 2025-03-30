// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER
// ISSUE: KT-13712

open class A {
    fun f(x: Any) {
    }
}

interface B<T> {
    fun f(x: T) {
    }
}

<!CONFLICTING_INHERITED_JVM_DECLARATIONS!>class C : B<String>, A()<!>
