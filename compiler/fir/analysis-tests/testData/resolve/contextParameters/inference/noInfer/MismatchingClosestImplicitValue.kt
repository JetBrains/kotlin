// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContextParameters
// DONT_WARN_ON_ERROR_SUPPRESSION
// ISSUE: KT-76771

@Suppress("INVISIBLE_REFERENCE")
context(_: @kotlin.internal.NoInfer T) fun <T> function() {}

@Suppress("INVISIBLE_REFERENCE")
context(_: @kotlin.internal.NoInfer T) fun <T> function2(t: Box<T>) {}

class KlassA
class KlassB

class Box<T>

fun main() {
    with(KlassA()) {
        with(KlassB()) {
            <!TYPE_MISMATCH!>function<!><KlassA>()
            <!TYPE_MISMATCH!>function2<!>(Box<KlassA>())
        }
    }
    context(KlassA()) {
        context(KlassB()) {
            <!TYPE_MISMATCH!>function<!><KlassA>()
            <!TYPE_MISMATCH!>function2<!>(Box<KlassA>())
        }
    }
}

fun <T, R> with(receiver: T, block: T.() -> R): R = receiver.block()
fun <T, R> context(with: T, block: context(T) () -> R): R = block(with)
