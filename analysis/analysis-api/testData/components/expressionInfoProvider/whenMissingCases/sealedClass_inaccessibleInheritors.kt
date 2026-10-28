// MODULE: dependency
// FILE: Base.kt
package lib

sealed interface Base {
    object A : Base
    class B : Base
    private object Private : Base
}

internal class Internal : Base

private class Outer {
    class InPrivateOuter : Base
}

internal enum class InternalEnum : Base {
    X, Y
}

// MODULE: main(dependency)
// FILE: main.kt
import lib.Base

fun test(b: Base) {
    <caret>when (b) {
    }
}
