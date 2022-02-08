// FIR_IDENTICAL
//KT-2096 Abstract property with no type specified causes compiler to crash

package c

abstract class Foo{
    <!PROPERTY_WITH_NO_TYPE_NO_INITIALIZER!>protected abstract val prop<!>
}