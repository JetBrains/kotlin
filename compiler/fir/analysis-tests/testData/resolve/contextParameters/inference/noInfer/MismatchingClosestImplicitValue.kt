// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters
// DONT_WARN_ON_ERROR_SUPPRESSION
// ISSUE: KT-76771

@Suppress("INVISIBLE_REFERENCE")
context(_: @kotlin.internal.NoInfer T) fun <T> function() {}

class KlassA
class KlassB

fun main() {
    with(KlassA()) {
        with(KlassB()) {
            <!TYPE_MISMATCH!>function<!><KlassA>()
        }
    }
    context(KlassA()) {
        context(KlassB()) {
            <!TYPE_MISMATCH!>function<!><KlassA>()
        }
    }
}

fun <T, R> with(receiver: T, block: T.() -> R): R = receiver.block()
fun <T, R> context(with: T, block: context(T) () -> R): R = block(with)
