// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-75061
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

sealed class SealedClass {
    open class SealedInheritor1(val prop1: String = "1"): SealedClass()
    class SealedInheritor2(val prop2: Int = 2): SealedClass()

    sealed class SealedSealedInheritor1(val prop3: Boolean = true): SealedClass()

    class IndirectSealedInheritor: SealedInheritor1()

    class Nested {
        class DeepSealedNestedInheritor: SealedClass()
        class DeepIndirectSealedNestedInheritor: SealedInheritor1()
    }

    inner class Inner(val innerProp: String = "inner prop"): SealedClass()

    class Nested1
    class Nested2
    sealed class SealedNested

    companion object Companion
}

fun <T>consume(arg: T): Nothing = TODO()

fun testUnsafeTypeCast2(i: SealedClass) {
    i as SealedInheritor1
    i.prop1.hashCode()

    i as <!UNRESOLVED_REFERENCE!>SealedInheritor2<!>
    i.<!UNRESOLVED_REFERENCE!>prop2<!>.dec()

    i as <!UNRESOLVED_REFERENCE!>SealedInheritor1<!>
    i.prop1.hashCode()
}

fun testUnsafeTypeCast(i: SealedClass) {
    (i as SealedInheritor1).prop1.hashCode()

    i as <!UNRESOLVED_REFERENCE!>SealedInheritor2<!>
    i.<!UNRESOLVED_REFERENCE!>prop2<!>.dec()

    var v = i as <!UNRESOLVED_REFERENCE!>SealedInheritor1<!>
    v.<!UNRESOLVED_REFERENCE!>prop1<!>.hashCode()

    var v2: SealedClass = i as <!UNRESOLVED_REFERENCE!>SealedInheritor1<!>
    v2.<!UNRESOLVED_REFERENCE!>prop1<!>.hashCode()

    var v3: <!UNRESOLVED_REFERENCE!>SealedInheritor1<!> = i as <!UNRESOLVED_REFERENCE!>SealedInheritor1<!>
    v3.<!UNRESOLVED_REFERENCE!>prop1<!>.hashCode()

    consume<SealedClass>(i as <!UNRESOLVED_REFERENCE!>SealedInheritor1<!>)
    consume<SealedClass.SealedInheritor1>(i as <!UNRESOLVED_REFERENCE!>SealedInheritor1<!>)

}

fun testSafeTypeCast(i: SealedClass) {
    (i as? SealedInheritor1)?.prop1?.hashCode()

    i as? SealedInheritor2
    i<!UNNECESSARY_SAFE_CALL!>?.<!><!UNRESOLVED_REFERENCE!>prop2<!>?.dec()

    var v = i as? SealedInheritor1
    v?.prop1?.hashCode()

    var v2: SealedClass? = i as? SealedInheritor1
    v2?.<!UNRESOLVED_REFERENCE!>prop1<!>?.hashCode()

    var v3: <!UNRESOLVED_REFERENCE!>SealedInheritor1<!>? = i as? SealedInheritor1
    v3?.<!UNRESOLVED_REFERENCE!>prop1<!>?.hashCode()

    consume<SealedClass?>(i as? SealedInheritor1)
    consume<SealedClass.SealedInheritor1?>(i as? SealedInheritor1)
    consume<SealedClass.SealedInheritor2?>(i as? SealedInheritor2)
}
