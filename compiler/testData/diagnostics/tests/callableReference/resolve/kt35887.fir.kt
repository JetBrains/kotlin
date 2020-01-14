// SKIP_TXT
// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_PARAMETER
import kotlin.reflect.KProperty0
import kotlin.reflect.KProperty1

object A

fun <T : Any, TProperty : Any?> property(property: KProperty1<T, TProperty>) = A

fun <TProperty> property(property: KProperty0<TProperty>) = A

val <K> K.key get() : A = property(Map.Entry<K, *>::key) // overload resolution ambiguity in the NI, OK in the OI
