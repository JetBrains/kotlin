// MODULE: dependency
// FILE: Desc.kt
package lib

sealed interface Desc

sealed interface PartiallyHidden : Desc
class Visible : PartiallyHidden
internal class Hidden : PartiallyHidden

class StringDesc : Desc

// MODULE: main(dependency)
// FILE: main.kt
import lib.*

fun test(d: Desc) {
    <caret>when (d) {
        is Visible -> {}
    }
}
