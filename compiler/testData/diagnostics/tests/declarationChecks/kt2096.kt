//KT-2096 Abstract property with no type specified causes compiler to crash

package c

abstract class Foo{
    <!PROPERTY_WITH_NO_TYPE_NO_INITIALIZER!>protected abstract val prop<!>
    protected abstract val prop2 <!ABSTRACT_DELEGATED_PROPERTY!>by <!DELEGATE_SPECIAL_FUNCTION_MISSING!>TODO()<!><!>
    protected abstract val prop3 = <!ABSTRACT_PROPERTY_WITH_INITIALIZER!>1<!>
}

interface Bar {
    <!PROPERTY_WITH_NO_TYPE_NO_INITIALIZER!>val prop<!>
    val prop2 <!DELEGATED_PROPERTY_IN_INTERFACE!>by <!DELEGATE_SPECIAL_FUNCTION_MISSING!>TODO()<!><!>
    val prop3 = <!PROPERTY_INITIALIZER_IN_INTERFACE!>1<!>
}
