// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// DONT_WARN_ON_ERROR_SUPPRESSION
import kotlin.reflect.KProperty0

class Data(val stringVal: String)

val data = Data("")

val tlValBoundVal by data::stringVal

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
fun <V> checkDelegate0(delegated: KProperty0<@kotlin.internal.NoInfer V>, source: KProperty0<V>) {}

fun test() {
    checkDelegate0(::tlValBoundVal, data::stringVal)
}