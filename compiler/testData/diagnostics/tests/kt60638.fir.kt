package usage

class MyType
class MyClass
val MyClass.isInterface get() = 4

fun usage(type: MyType) {
    type.<!UNRESOLVED_REFERENCE!>isInterface<!>()
}
