fun fn(): Nothing = throw java.lang.<!UNRESOLVED_REFERENCE!>RuntimeException<!>("oops")

val x: Nothing = throw java.lang.<!UNRESOLVED_REFERENCE!>RuntimeException<!>("oops")

class SomeClass {
    fun method() {
        throw java.lang.<!UNRESOLVED_REFERENCE!>AssertionError<!>("!!!")
    }
}