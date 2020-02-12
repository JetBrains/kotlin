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
    with.NestedWithClassObject
    with.<!UNRESOLVED_REFERENCE!>NestedWithClassObject<!>()
    with.NestedWithClassObject.foo()
    with.NestedEnum.A
    with.NestedObj
    with.NestedObj()
    with.NestedObj.foo()

    without.<!UNRESOLVED_REFERENCE!>Nested<!>()
    without.NestedWithClassObject
    without.<!UNRESOLVED_REFERENCE!>NestedWithClassObject<!>()
    without.NestedWithClassObject.foo()
    without.NestedEnum.A
    without.NestedObj
    without.NestedObj()
    without.NestedObj.foo()

    obj.<!UNRESOLVED_REFERENCE!>Nested<!>()
    obj.NestedWithClassObject
    obj.<!UNRESOLVED_REFERENCE!>NestedWithClassObject<!>()
    obj.NestedWithClassObject.foo()
    obj.NestedEnum.A
    obj.NestedObj
    obj.NestedObj()
    obj.NestedObj.foo()
}