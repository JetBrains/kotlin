// "Change return type of current function 'Companion.foo' to 'Int'" "true"
package foo.bar

class A {
    companion object {
        fun foo(): String {
            return <caret>1
        }
    }
}

