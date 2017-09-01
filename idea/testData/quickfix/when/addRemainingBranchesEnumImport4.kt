// "Add remaining branches with import" "true"
// WITH_RUNTIME
import Foo.*

enum class Foo {
    A, B, C
}

enum class Baz {
    AA, B, CC
}

class Test {
    fun foo(e: Foo) {
        when (e) {
            A -> TODO()
            B -> TODO()
            C -> TODO()
        }
    }
    fun baz(e: Baz) {
        when<caret> (e) {
        }
    }
}
