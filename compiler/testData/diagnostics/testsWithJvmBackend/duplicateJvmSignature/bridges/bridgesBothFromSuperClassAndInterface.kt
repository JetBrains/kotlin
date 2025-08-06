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

<!ACCIDENTAL_OVERRIDE_BY_BRIDGE_METHOD_WARNING!>class C : A<String>(), B<Int><!>
<!ACCIDENTAL_OVERRIDE_BY_BRIDGE_METHOD_WARNING!>class D : A<String>(), B<String?><!>
<!ACCIDENTAL_OVERRIDE_BY_BRIDGE_METHOD_WARNING!>class E : A<String?>(), B<String><!>
class F<Q> : A<String>(), B<Q>
class G<P, Q> : A<P>(), B<Q>
<!ACCIDENTAL_OVERRIDE_BY_BRIDGE_METHOD_WARNING!>class H<P> : A<P>(), B<String><!>

