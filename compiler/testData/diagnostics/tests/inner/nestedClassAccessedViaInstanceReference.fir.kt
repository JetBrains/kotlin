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
    with.<!UNRESOLVED_REFERENCE!>NestedWithClassObject<!>
    with.<!UNRESOLVED_REFERENCE!>NestedWithClassObject<!>()
    with.<!UNRESOLVED_REFERENCE!>NestedWithClassObject<!>.foo()
    with.<!UNRESOLVED_REFERENCE!>NestedEnum<!>.A
    with.<!UNRESOLVED_REFERENCE!>NestedObj<!>
    with.<!UNRESOLVED_REFERENCE!>NestedObj<!>()
    with.<!UNRESOLVED_REFERENCE!>NestedObj<!>.foo()

    without.<!UNRESOLVED_REFERENCE!>Nested<!>()
    without.<!UNRESOLVED_REFERENCE!>NestedWithClassObject<!>
    without.<!UNRESOLVED_REFERENCE!>NestedWithClassObject<!>()
    without.<!UNRESOLVED_REFERENCE!>NestedWithClassObject<!>.foo()
    without.<!UNRESOLVED_REFERENCE!>NestedEnum<!>.A
    without.<!UNRESOLVED_REFERENCE!>NestedObj<!>
    without.<!UNRESOLVED_REFERENCE!>NestedObj<!>()
    without.<!UNRESOLVED_REFERENCE!>NestedObj<!>.foo()

    obj.<!UNRESOLVED_REFERENCE!>Nested<!>()
    obj.<!UNRESOLVED_REFERENCE!>NestedWithClassObject<!>
    obj.<!UNRESOLVED_REFERENCE!>NestedWithClassObject<!>()
    obj.<!UNRESOLVED_REFERENCE!>NestedWithClassObject<!>.foo()
    obj.<!UNRESOLVED_REFERENCE!>NestedEnum<!>.A
    obj.<!UNRESOLVED_REFERENCE!>NestedObj<!>
    obj.<!UNRESOLVED_REFERENCE!>NestedObj<!>()
    obj.<!UNRESOLVED_REFERENCE!>NestedObj<!>.foo()
}
