// LANGUAGE: +FullValueClasses
package test

abstract value class Base {
    abstract val value: Int

    fun doubled(): Int = value * 2
}

class RegularClass(override var value: Int) : Base()

// class: test/RegularClass
