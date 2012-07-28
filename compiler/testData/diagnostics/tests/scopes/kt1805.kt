package kt1805
//KT-1805 Better diagnostic for access to private field of parent class

open class Some {
    private val privateField = 12
}

class SomeSubclass : Some() {
    fun test() {
        this.<!INVISIBLE_MEMBER!>privateField<!> // 1. Unresolved reference
    }
}

fun test() {
    val s2 = Some()
    s2.<!INVISIBLE_MEMBER!>privateField<!> // 2. Can't access to 'privateField' in Some

    val s1 = SomeSubclass()
    s1.<!INVISIBLE_MEMBER!>privateField<!> // 3. Unresolved reference
}