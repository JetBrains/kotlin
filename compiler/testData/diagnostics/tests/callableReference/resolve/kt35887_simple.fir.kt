// RUN_PIPELINE_TILL: FRONTEND
// SKIP_TXT
// DIAGNOSTICS: -UNUSED_PARAMETER
import kotlin.reflect.KProperty0

object A

fun <TProperty> property0(property: KProperty0<TProperty>) = A
val <K> K.key get() : A = <!CANNOT_INFER_PARAMETER_TYPE!>property0<!>(Map.Entry<K, *>::<!INAPPLICABLE_CANDIDATE!>key<!>) // should be forbidden
