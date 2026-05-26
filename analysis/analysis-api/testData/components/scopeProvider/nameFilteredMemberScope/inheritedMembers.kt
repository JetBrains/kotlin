// NAME: equals
// NAME: foo
package test

abstract class Base {
    fun foo(): Int = 5
    fun bar(): String = ""
}

class C : Base() {
    fun baz() {}
}

// class: test/C
