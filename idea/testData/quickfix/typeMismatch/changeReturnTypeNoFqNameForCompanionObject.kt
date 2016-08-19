// "Change 'Companion.foo' function return type to 'Int'" "true"
package foo.bar

class A {
    companion object {
        fun foo(): String {
            return <caret>1
        }
    }
}

