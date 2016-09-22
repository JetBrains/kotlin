// "Change return type of enclosing function 'B.foo' to 'Int'" "true"
package foo.bar

class A {
    class B {
        fun foo(): String {
            return <caret>1
        }
    }
}

