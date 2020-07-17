// FLOW: IN

package sample

actual class ExpectClass {
    actual fun foo(p: Any) {
        println(<caret>p)
    }
}
