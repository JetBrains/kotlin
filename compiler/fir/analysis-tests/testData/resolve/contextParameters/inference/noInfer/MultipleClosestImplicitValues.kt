// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContextParameters
// DONT_WARN_ON_ERROR_SUPPRESSION
// ISSUE: KT-76772

@Suppress("INVISIBLE_REFERENCE")
context(_: @kotlin.internal.NoInfer T) fun <T> function() {}

@Suppress("INVISIBLE_REFERENCE")
context(_: @kotlin.internal.NoInfer T) fun <T> function2(t: Box<T>) {}

@Suppress("INVISIBLE_REFERENCE")
context(_: Box<@kotlin.internal.NoInfer T>) fun <T> function3() {}

class Box<T>

class KlassA
class KlassB

fun main() {
    context(KlassA(), KlassB()) {
        <!AMBIGUOUS_CONTEXT_ARGUMENT!>function<!><KlassA>()
        <!AMBIGUOUS_CONTEXT_ARGUMENT!>function2<!>(Box<KlassA>())
    }
    context(Box<KlassA>(), Box<KlassB>()) {
        <!AMBIGUOUS_CONTEXT_ARGUMENT!>function3<!><KlassA>()
        <!AMBIGUOUS_CONTEXT_ARGUMENT!>function3<!><KlassB>()
    }
}

fun <T1, T2, R> context(with1: T1, with2: T2, block: context(T1, T2) () -> R): R = block(with1, with2)
