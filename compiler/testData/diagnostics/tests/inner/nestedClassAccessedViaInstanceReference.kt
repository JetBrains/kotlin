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
    with.<!UNRESOLVED_REFERENCE!>Nested<!>()
    with.<!NO_COMPANION_OBJECT, NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE!>NestedWithClassObject<!>
    with.<!UNRESOLVED_REFERENCE!>NestedWithClassObject<!>()
    with.<!NO_COMPANION_OBJECT, NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE!>NestedWithClassObject<!>.foo()
    with.<!NO_COMPANION_OBJECT, NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE!>NestedEnum<!>.A
    with.<!NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE!>NestedObj<!>
    with.<!RESOLUTION_TO_CLASSIFIER!>NestedObj<!>()
    with.<!NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE!>NestedObj<!>.foo()

    without.<!UNRESOLVED_REFERENCE!>Nested<!>()
    without.<!NO_COMPANION_OBJECT, NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE!>NestedWithClassObject<!>
    without.<!UNRESOLVED_REFERENCE!>NestedWithClassObject<!>()
    without.<!NO_COMPANION_OBJECT, NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE!>NestedWithClassObject<!>.foo()
    without.<!NO_COMPANION_OBJECT, NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE!>NestedEnum<!>.A
    without.<!NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE!>NestedObj<!>
    without.<!RESOLUTION_TO_CLASSIFIER!>NestedObj<!>()
    without.<!NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE!>NestedObj<!>.foo()

    obj.<!UNRESOLVED_REFERENCE!>Nested<!>()
    obj.<!NO_COMPANION_OBJECT, NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE!>NestedWithClassObject<!>
    obj.<!UNRESOLVED_REFERENCE!>NestedWithClassObject<!>()
    obj.<!NO_COMPANION_OBJECT, NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE!>NestedWithClassObject<!>.foo()
    obj.<!NO_COMPANION_OBJECT, NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE!>NestedEnum<!>.A
    obj.<!NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE!>NestedObj<!>
    obj.<!RESOLUTION_TO_CLASSIFIER!>NestedObj<!>()
    obj.<!NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE!>NestedObj<!>.foo()
}