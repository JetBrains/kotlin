package usage

class MyType
class MyClass
val MyClass.isInterface get() = 4

fun usage(type: MyType) {
    type.<!FUNCTION_EXPECTED, UNRESOLVED_REFERENCE_WRONG_RECEIVER!>isInterface<!>()
}
