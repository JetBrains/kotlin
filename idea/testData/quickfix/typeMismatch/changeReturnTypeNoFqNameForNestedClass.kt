// "Change 'B.foo' function return type to 'Int'" "true"
package foo.bar

class A {
    class B {
        fun foo(): String {
            return <caret>1
        }
    }
}

