// FIR_IDENTICAL
// ISSUE: KT-65959
import kotlin.reflect.KFunction0

<!NOTHING_TO_INLINE!>inline<!> fun foo(block: KFunction0<Unit>) {
    block()
}
