// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-73476

class Class
private class PrivateClass

open class OuterClass {
    inner class InnerClass
    private inner class InnerPrivateClass

    private typealias PrivateTAInner = InnerClass
    protected typealias ProtectedTAInner = InnerClass
    typealias PublicTAInner = InnerClass

    private typealias PrivateTA = Class
    protected typealias ProtectedTA = Class
    typealias PublicTA = Class

    private typealias PrivateTAPrivateInner = InnerPrivateClass
    protected typealias <!EXPOSED_TYPEALIAS_EXPANDED_TYPE!>ProtectedTAPrivateInner<!> = InnerPrivateClass // ERROR (exposed type)
    typealias <!EXPOSED_TYPEALIAS_EXPANDED_TYPE!>PublicTAPrivateInner<!> = InnerPrivateClass // ERROR (exposed type)

    private typealias PrivateTAPrivateClass = PrivateClass
    protected typealias <!EXPOSED_TYPEALIAS_EXPANDED_TYPE!>ProtectedTAPrivateClass<!> = PrivateClass // ERROR (exposed type)
    typealias <!EXPOSED_TYPEALIAS_EXPANDED_TYPE!>PublicTAPrivateClass<!> = PrivateClass // ERROR (exposed type)

    fun testPrivate() {
        PrivateTAInner() // OK
        ProtectedTAInner() // OK
        PublicTAInner() // OK

        PrivateTA() // OK
        ProtectedTA() // OK
        PublicTA() // OK

        PrivateTAPrivateInner() // OK
        ProtectedTAPrivateInner() // OK
        PublicTAPrivateInner() // OK
    }
}

private typealias PrivateOuterTA = OuterClass.InnerClass
typealias PublicOuterTA = OuterClass.InnerClass

class OuterClassInheritor : OuterClass() {
    fun testProtected() {
        <!INVISIBLE_REFERENCE!>PrivateTAInner<!>() // ERROR (invisible)
        ProtectedTAInner() // OK
        PublicTAInner() // OK

        <!INVISIBLE_REFERENCE!>PrivateTA<!>()  // ERROR (invisible)
        ProtectedTA() // OK
        PublicTA() // OK

        <!INVISIBLE_REFERENCE!>PrivateTAPrivateInner<!>() // ERROR (invisible)
        <!INVISIBLE_REFERENCE!>ProtectedTAPrivateInner<!>() // ERROR (invisible)
        <!INVISIBLE_REFERENCE!>PublicTAPrivateInner<!>() // ERROR (invisible)
    }
}

fun testPublic() {
    val outerClass = OuterClass()
    outerClass.<!UNRESOLVED_REFERENCE!>PrivateTAInner<!>() // ERROR (UNRESOLVED_REFERENCE)
    outerClass.<!UNRESOLVED_REFERENCE!>ProtectedTAInner<!>() // ERROR (UNRESOLVED_REFERENCE)
    outerClass.<!UNRESOLVED_REFERENCE!>PublicTAInner<!>() // ERROR (UNRESOLVED_REFERENCE)

    outerClass.<!UNRESOLVED_REFERENCE!>PrivateTAPrivateInner<!>() // ERROR (UNRESOLVED_REFERENCE)

    OuterClass.<!INVISIBLE_REFERENCE!>PrivateTA<!>() // ERROR (invisible)
    OuterClass.<!INVISIBLE_REFERENCE!>ProtectedTA<!>() // ERROR (invisible)
    OuterClass.PublicTA() // OK
}
