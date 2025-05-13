// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters
// DONT_WARN_ON_ERROR_SUPPRESSION
// ISSUE: KT-76772

@Suppress("INVISIBLE_REFERENCE")
context(_: @kotlin.internal.NoInfer T) fun <T> function() {}

class KlassA
class KlassB

fun main() {
    context(KlassA(), KlassB()) {
        <!AMBIGUOUS_CONTEXT_ARGUMENT!>function<!><KlassA>()
    }
}

fun <T1, T2, R> context(with1: T1, with2: T2, block: context(T1, T2) () -> R): R = block(with1, with2)
