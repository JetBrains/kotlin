// FULL_JDK
// WITH_STDLIB
// WITH_REFLECT

interface A
interface B : A
interface C

fun test1(a: A) {
    when (a.<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>javaClass<!>) {
        A::class.<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>java<!> -> {}
        B::class.<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>java<!> -> {}
        C::class.<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>java<!> -> {}
        Any::class.<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>java<!> -> {}
        else -> {}
    }
}

class Foo : B
class Bar

fun test2(f: Foo) {
    when (f.<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>javaClass<!>) {
        Foo::class.<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>java<!> -> {}
        Bar::class.<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>java<!> -> {}
        A::class.<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>java<!> -> {}
        B::class.<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>java<!> -> {}
        C::class.<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>java<!> -> {}
        Any::class.<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>java<!> -> {}
    }
}
