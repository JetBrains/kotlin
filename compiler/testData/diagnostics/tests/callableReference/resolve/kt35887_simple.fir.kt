// SKIP_TXT
// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_PARAMETER
import kotlin.reflect.KProperty0

object A

fun <TProperty> property0(property: KProperty0<TProperty>) = A
val <K> K.key get() : A = <!INAPPLICABLE_CANDIDATE!>property0<!>(Map.Entry<K, *>::key) // should be forbidden
