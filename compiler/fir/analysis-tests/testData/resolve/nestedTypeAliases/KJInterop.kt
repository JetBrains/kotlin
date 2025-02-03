// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// SKIP_FIR_DUMP

// FILE: JClass.java
public class JClass {
    public class PublicInner {}
    protected class ProtectedInner {}
    private class PrivateInner {}

    public static class PublicStatic {}
    protected static class ProtectedStatic {}
    private static class PrivateStatic {}
}

// FILE: JGeneric.java
public class JGeneric<T> {
    public class PublicInner {}
    protected class ProtectedInner {}
    private class PrivateInner {}
}

// FILE: JRecord.java
public record JRecord() {}

// FILE: main.kt

open class KJInteropClass {
    typealias JClassTA = JClass
    typealias JClassPublicInnerTA = JClass.PublicInner
    typealias JClassPublicStaticTA = JClass.PublicStatic

    typealias JGenericTA<T> = JGeneric<T>
    typealias JGenericPublicInnerTA<T> = JGeneric<T>.PublicInner

    typealias JRecordTA = JRecord

    fun testJClassTAResolution(i: JClassTA): JClass = i
    fun testJClassPublicInnerTAResolution(i: JClassPublicInnerTA): JClass.PublicInner = i
    fun testJClassPublicStaticTAResolution(i: JClassPublicStaticTA): JClass.PublicStatic = i

    fun testJGenericTAResolution(i: JGenericTA<KJInteropClass>): JGeneric<KJInteropClass> = i
    fun testJGenericPublicInnerTAResolution(i: JGenericPublicInnerTA<KJInteropClass>): JGeneric<KJInteropClass>.PublicInner = i
}

class KJInteropSubJClass : JClass() {
    typealias PublicStatic = String

    fun testConflictingTA(arg: PublicStatic) {
        arg.uppercase()
    }
}

class NegativesHolder {
    typealias <!EXPOSED_TYPEALIAS_EXPANDED_TYPE!>JClassProtectedInnerTA<!> = JClass.ProtectedInner
    typealias <!EXPOSED_TYPEALIAS_EXPANDED_TYPE!>JClassPrivateInnerTA<!> = JClass.<!INVISIBLE_REFERENCE!>PrivateInner<!>
    typealias <!EXPOSED_TYPEALIAS_EXPANDED_TYPE!>JClassProtectedStaticTA<!> = JClass.ProtectedStatic
    typealias <!EXPOSED_TYPEALIAS_EXPANDED_TYPE!>JClassPrivateStaticTA<!> = JClass.<!INVISIBLE_REFERENCE!>PrivateStatic<!>
    typealias <!EXPOSED_TYPEALIAS_EXPANDED_TYPE!>JGenericProtectedInnerTA<!><T> = JGeneric<T>.ProtectedInner
    typealias <!EXPOSED_TYPEALIAS_EXPANDED_TYPE!>JGenericPrivateInnerTA<!><T> = JGeneric<T>.<!INVISIBLE_REFERENCE!>PrivateInner<!>

    fun <!EXPOSED_FUNCTION_RETURN_TYPE!>testJClassProtectedInnerTAResolution<!>(<!EXPOSED_PARAMETER_TYPE!>i: JClassProtectedInnerTA<!>): JClass.ProtectedInner = i
    fun <!EXPOSED_FUNCTION_RETURN_TYPE!>testJClassProtectedStaticTAResolution<!>(<!EXPOSED_PARAMETER_TYPE!>i: JClassProtectedStaticTA<!>): JClass.ProtectedStatic = i
    fun <!EXPOSED_FUNCTION_RETURN_TYPE!>testJGenericProtectedInnerTAResolution<!>(<!EXPOSED_PARAMETER_TYPE!>i: JGenericProtectedInnerTA<<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Class<!>><!>): JGeneric<<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Class<!>>.ProtectedInner = i

}
