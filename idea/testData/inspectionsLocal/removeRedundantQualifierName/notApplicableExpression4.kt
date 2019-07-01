// PROBLEM: none
package foo

class Foo {
    fun test() {
        <caret>foo.myRun {
            42
        }
    }
}

inline fun <R> myRun(block: () -> R): R = block()

inline fun <T, R> T.myRun(block: T.() -> R): R = block()