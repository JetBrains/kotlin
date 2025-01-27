// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-7972
// WITH_STDLIB
// DIAGNOSTICS: -DEBUG_INFO_SMARTCAST
// LATEST_LV_DIFFERENCE

fun <E> Iterable<E>.windowed() = this is RandomAccess && this is List

abstract class TransformingSequence<T, R> : Sequence<R>

fun <T> Sequence<T>.flatten() = this is TransformingSequence<*, *> && this is <!CANNOT_CHECK_FOR_ERASED_DEPRECATION_WARNING!>TransformingSequence<*, T><!>
