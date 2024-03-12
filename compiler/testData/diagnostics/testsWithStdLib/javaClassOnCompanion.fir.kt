package test

class A {
    companion object
}

object O

enum class E {
    ENTRY
}


val a0 = A.<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>javaClass<!>
val a1 = test.A.<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>javaClass<!>
val a2 = A.Companion.<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>javaClass<!>
val a3 = A::class.<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>java<!>
val a4 = test.A::class.<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>java<!>
val a5 = A.Companion::class.<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>java<!>

val o0 = O.<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>javaClass<!>
val o1 = O::class.<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>java<!>

val e0 = E.<!UNRESOLVED_REFERENCE!>javaClass<!>
val e1 = E::class.<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>java<!>
val e2 = E.ENTRY.<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>javaClass<!>

val int0 = Int.<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>javaClass<!>
val int1 = Int::class.<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>java<!>

val string0 = String.<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>javaClass<!>
val string1 = String::class.<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>java<!>
