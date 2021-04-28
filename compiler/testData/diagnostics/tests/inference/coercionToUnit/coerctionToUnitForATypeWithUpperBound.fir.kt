// !DIAGNOSTICS: -UNUSED_PARAMETER

interface Base
class Foo : Base

fun <T : Base> myRun(action: () -> T): T = action()

fun foo(f: (Foo) -> Unit) {}

fun main() {
    foo { f: Foo ->
        <!NEW_INFERENCE_ERROR!>myRun { f }<!>
    }
}
