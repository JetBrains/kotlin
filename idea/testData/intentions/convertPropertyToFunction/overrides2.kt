// SHOULD_FAIL_WITH: Property overloaded in child class constructor
package test

open class Parent() {
    open val <caret>o: String = ""
}

class Child(override val o: String) : Parent()