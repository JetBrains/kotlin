trait N { fun foo() = 1 }

class WithClassObject {
    default object {}

    class Nested()
    class NestedWithClassObject { default object : N }
    enum class NestedEnum { A }
    object NestedObj : N { fun invoke() = 1 }
}

class WithoutClassObject {
    class Nested()
    class NestedWithClassObject { default object : N }
    enum class NestedEnum { A }
    object NestedObj : N { fun invoke() = 1 }
}

object Obj {
    class Nested()
    class NestedWithClassObject { default object : N }
    enum class NestedEnum { A }
    object NestedObj : N { fun invoke() = 1 }
}

fun test(with: WithClassObject, without: WithoutClassObject, obj: Obj) {
    with.<!NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE!>Nested<!>()
    with.<!NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE!>NestedWithClassObject<!>
    with.<!NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE!>NestedWithClassObject<!>()
    with.<!NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE!>NestedWithClassObject<!>.foo()
    with.<!NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE!>NestedEnum<!>.A
    with.<!NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE!>NestedObj<!>
    with.<!NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE!>NestedObj<!>()
    with.<!NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE!>NestedObj<!>.foo()

    without.<!NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE!>Nested<!>()
    without.<!NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE!>NestedWithClassObject<!>
    without.<!NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE!>NestedWithClassObject<!>()
    without.<!NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE!>NestedWithClassObject<!>.foo()
    without.<!NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE!>NestedEnum<!>.A
    without.<!NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE!>NestedObj<!>
    without.<!NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE!>NestedObj<!>()
    without.<!NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE!>NestedObj<!>.foo()

    obj.<!NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE!>Nested<!>()
    obj.<!NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE!>NestedWithClassObject<!>
    obj.<!NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE!>NestedWithClassObject<!>()
    obj.<!NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE!>NestedWithClassObject<!>.foo()
    obj.<!NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE!>NestedEnum<!>.A
    obj.<!NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE!>NestedObj<!>
    obj.<!NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE!>NestedObj<!>()
    obj.<!NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE!>NestedObj<!>.foo()
}

