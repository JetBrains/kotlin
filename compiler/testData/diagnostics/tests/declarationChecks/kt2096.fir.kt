//KT-2096 Abstract property with no type specified causes compiler to crash

package c

abstract class Foo{
    protected abstract <!ABSTRACT_PROPERTY_WITHOUT_TYPE!>val prop<!>
    protected abstract val prop2 by <!ABSTRACT_DELEGATED_PROPERTY, DELEGATE_SPECIAL_FUNCTION_MISSING!>TODO()<!>
    protected abstract val prop3 = <!ABSTRACT_PROPERTY_WITH_INITIALIZER!>1<!>
}

interface Bar {
    <!ABSTRACT_PROPERTY_WITHOUT_TYPE!>val prop<!>
    val prop2 by <!DELEGATED_PROPERTY_IN_INTERFACE, DELEGATE_SPECIAL_FUNCTION_MISSING!>TODO()<!>
    val prop3 = <!PROPERTY_INITIALIZER_IN_INTERFACE!>1<!>
}