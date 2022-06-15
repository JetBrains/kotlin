// FIR_IDENTICAL
class Test1 : <!CLASS_CANNOT_BE_EXTENDED_DIRECTLY!>ExternalEnum<!>

class Outer {
    class Test2 : <!CLASS_CANNOT_BE_EXTENDED_DIRECTLY!>ExternalEnum<!>
}

fun outer() {
    class Test3 : <!CLASS_CANNOT_BE_EXTENDED_DIRECTLY!>ExternalEnum<!>
}