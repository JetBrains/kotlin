// !DIAGNOSTICS_NUMBER: 4
// !DIAGNOSTICS: NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE

class A {
    class Nested()
    class NestedWithClassObject { default object }
    enum class NestedEnum { A }
    object NestedObj { fun invoke() = 1 }
}

fun test(a: A) {
    a.Nested()
    a.NestedWithClassObject
    a.NestedEnum
    a.NestedObj
}