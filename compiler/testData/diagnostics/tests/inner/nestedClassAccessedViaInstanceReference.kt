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
    with.<!NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE, NO_COMPANION_OBJECT!>NestedWithClassObject<!>
    with.<!UNRESOLVED_REFERENCE!>NestedWithClassObject<!>()
    with.<!NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE, NO_COMPANION_OBJECT!>NestedWithClassObject<!>.foo()
    with.<!NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE, NO_COMPANION_OBJECT!>NestedEnum<!>.A
    with.<!NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE!>NestedObj<!>
    with.<!RESOLUTION_TO_CLASSIFIER!>NestedObj<!>()
    with.<!NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE!>NestedObj<!>.foo()

    without.<!UNRESOLVED_REFERENCE!>Nested<!>()
    without.<!NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE, NO_COMPANION_OBJECT!>NestedWithClassObject<!>
    without.<!UNRESOLVED_REFERENCE!>NestedWithClassObject<!>()
    without.<!NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE, NO_COMPANION_OBJECT!>NestedWithClassObject<!>.foo()
    without.<!NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE, NO_COMPANION_OBJECT!>NestedEnum<!>.A
    without.<!NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE!>NestedObj<!>
    without.<!RESOLUTION_TO_CLASSIFIER!>NestedObj<!>()
    without.<!NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE!>NestedObj<!>.foo()

    obj.<!UNRESOLVED_REFERENCE!>Nested<!>()
    obj.<!NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE, NO_COMPANION_OBJECT!>NestedWithClassObject<!>
    obj.<!UNRESOLVED_REFERENCE!>NestedWithClassObject<!>()
    obj.<!NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE, NO_COMPANION_OBJECT!>NestedWithClassObject<!>.foo()
    obj.<!NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE, NO_COMPANION_OBJECT!>NestedEnum<!>.A
    obj.<!NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE!>NestedObj<!>
    obj.<!RESOLUTION_TO_CLASSIFIER!>NestedObj<!>()
    obj.<!NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE!>NestedObj<!>.foo()
}