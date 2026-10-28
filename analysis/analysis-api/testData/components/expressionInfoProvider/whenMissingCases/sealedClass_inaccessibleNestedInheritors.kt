// MODULE: dependency
// FILE: Desc.kt
package lib

sealed interface Desc

sealed interface ClassDesc : Desc
internal class PrimitiveClassDesc : ClassDesc
internal class ReferenceClassDesc : ClassDesc

sealed interface MethodHandleDesc : Desc
internal class AsTypeMethodHandleDesc : DynamicDesc(), MethodHandleDesc
sealed interface DirectMethodHandleDesc : MethodHandleDesc
internal class DirectMethodHandleDescImpl : DirectMethodHandleDesc

sealed interface PartiallyHidden : Desc
class Visible : PartiallyHidden
internal class Hidden : PartiallyHidden

abstract class DynamicDesc : Desc
class StringDesc : Desc

// MODULE: main(dependency)
// FILE: main.kt
import lib.Desc

fun test(d: Desc) {
    <caret>when (d) {
    }
}
