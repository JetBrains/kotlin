// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER
// LANGUAGE: -ForbidBridgesConflictingWithInheritedMethodsInJvmCode
// ISSUE: KT-13712

open class A<T> {
    fun foo(t: T) {
    }
}

interface B<T> {
    fun foo(t: T) {
    }
}

<!ACCIDENTAL_OVERRIDE_BY_BRIDGE_METHOD_ERROR!>class C : A<String>(), B<Int><!>
<!ACCIDENTAL_OVERRIDE_BY_BRIDGE_METHOD_ERROR!>class D : A<String>(), B<String?><!>
<!ACCIDENTAL_OVERRIDE_BY_BRIDGE_METHOD_ERROR!>class E : A<String?>(), B<String><!>
class F<Q> : A<String>(), B<Q>
class G<P, Q> : A<P>(), B<Q>
<!ACCIDENTAL_OVERRIDE_BY_BRIDGE_METHOD_ERROR!>class H<P> : A<P>(), B<String><!>

