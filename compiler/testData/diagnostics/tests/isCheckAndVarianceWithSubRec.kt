// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-7972
// WITH_STDLIB
// DIAGNOSTICS: -DEBUG_INFO_SMARTCAST

interface Inv<T>
interface InvImpl<T> : Inv<T>
interface InvString : Inv<String>

fun bar(t: InvString) {
    t <!UNCHECKED_CAST!>as InvImpl<String><!>
}
