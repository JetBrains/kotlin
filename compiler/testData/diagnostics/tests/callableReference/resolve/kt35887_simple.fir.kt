// SKIP_TXT
// !DIAGNOSTICS: -UNUSED_PARAMETER
import kotlin.reflect.KProperty0

object A

fun <TProperty> property0(property: KProperty0<TProperty>) = A
val <K> K.key get() : A = property0(Map.Entry<K, *>::<!UNRESOLVED_REFERENCE!>key<!>) // should be forbidden
