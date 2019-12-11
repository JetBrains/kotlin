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
    with.Nested()
    with.NestedWithClassObject
    with.NestedWithClassObject()
    with.NestedWithClassObject.foo()
    with.NestedEnum.A
    with.NestedObj
    with.<!INAPPLICABLE_CANDIDATE!>NestedObj<!>()
    with.NestedObj.foo()

    without.Nested()
    without.NestedWithClassObject
    without.NestedWithClassObject()
    without.NestedWithClassObject.foo()
    without.NestedEnum.A
    without.NestedObj
    without.<!INAPPLICABLE_CANDIDATE!>NestedObj<!>()
    without.NestedObj.foo()

    obj.Nested()
    obj.NestedWithClassObject
    obj.NestedWithClassObject()
    obj.NestedWithClassObject.foo()
    obj.NestedEnum.A
    obj.NestedObj
    obj.<!INAPPLICABLE_CANDIDATE!>NestedObj<!>()
    obj.NestedObj.foo()
}