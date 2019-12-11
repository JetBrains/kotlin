//KT-2096 Abstract property with no type specified causes compiler to crash

package c

abstract class Foo{
    protected abstract val prop
}