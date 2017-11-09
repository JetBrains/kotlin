// "Add remaining branches with import" "true"
// WITH_RUNTIME
import Foo.*

enum class Foo {
    A, B, C
}

class Test {
    fun foo(e: Foo) {
        when<caret> (e) {
            A, Foo.B -> TODO()
        }
    }
}
