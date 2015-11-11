interface N { fun foo() = 1 }

class WithClassObject {
    companion object {}

    class Nested()
    class NestedWithClassObject { companion object : N }
    enum class NestedEnum { A }
    object NestedObj : N { operator fun invoke() = 1 }
}

class WithoutClassObject {
    class Nested()
    class NestedWithClassObject { companion object : N }
    enum class NestedEnum { A }
    object NestedObj : N { operator fun invoke() = 1 }
}

object Obj {
    class Nested()
    class NestedWithClassObject { companion object : N }
    enum class NestedEnum { A }
    object NestedObj : N { operator fun invoke() = 1 }
}

fun test(with: WithClassObject, without: WithoutClassObject, obj: Obj) {
    with.<!NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE!>Nested<!>()
    with.<!NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE!>NestedWithClassObject<!>
    with.<!NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE!>NestedWithClassObject<!>()
    with.<!NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE!>NestedWithClassObject<!>.foo()
    with.<!INVISIBLE_MEMBER, NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE, FUNCTION_CALL_EXPECTED!>NestedEnum<!>.<!NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE!>A<!>
    with.<!NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE!>NestedObj<!>
    with.<!NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE!>NestedObj<!>()
    with.<!NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE!>NestedObj<!>.foo()

    without.<!NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE!>Nested<!>()
    without.<!NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE!>NestedWithClassObject<!>
    without.<!NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE!>NestedWithClassObject<!>()
    without.<!NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE!>NestedWithClassObject<!>.foo()
    without.<!INVISIBLE_MEMBER, NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE, FUNCTION_CALL_EXPECTED!>NestedEnum<!>.<!NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE!>A<!>
    without.<!NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE!>NestedObj<!>
    without.<!NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE!>NestedObj<!>()
    without.<!NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE!>NestedObj<!>.foo()

    obj.<!NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE!>Nested<!>()
    obj.<!NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE!>NestedWithClassObject<!>
    obj.<!NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE!>NestedWithClassObject<!>()
    obj.<!NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE!>NestedWithClassObject<!>.foo()
    obj.<!INVISIBLE_MEMBER, NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE, FUNCTION_CALL_EXPECTED!>NestedEnum<!>.<!NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE!>A<!>
    obj.<!NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE!>NestedObj<!>
    obj.<!NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE!>NestedObj<!>()
    obj.<!NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE!>NestedObj<!>.foo()
}