// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CompanionBlocksAndExtensions
import kotlin.reflect.*

class C {
    fun baz(x: Int) {}
}

companion fun C.foo() {}
companion fun C.bar(s: String) = s
companion fun C.baz(s: String) {}
companion val C.readonly = 1
companion var C.mutable = 1

fun test() {
    accept<(C) -> Unit>(C::<!INAPPLICABLE_CANDIDATE!>foo<!>)
    accept<(C, String) -> String>(C::<!INAPPLICABLE_CANDIDATE!>bar<!>)
    accept<(C, Int) -> Unit>(C::baz)

    accept<(C) -> Int>(C::<!INAPPLICABLE_CANDIDATE!>readonly<!>)
    accept<(C) -> Int>(C::<!INAPPLICABLE_CANDIDATE!>mutable<!>)
    accept<KProperty1<C, Int>>(C::<!INAPPLICABLE_CANDIDATE!>readonly<!>)
    accept<KProperty1<C, Int>>(C::<!INAPPLICABLE_CANDIDATE!>mutable<!>)
    accept<KMutableProperty1<C, Int>>(C::<!INAPPLICABLE_CANDIDATE!>mutable<!>)

    accept<() -> Unit>(C::foo)
    accept<(String) -> String>(C::bar)
    accept<(String) -> Unit>(C::baz)

    accept<() -> Int>(C::readonly)
    accept<() -> Int>(C::mutable)
    accept<KProperty0<Int>>(C::readonly)
    accept<KProperty0<Int>>(C::mutable)
    accept<KMutableProperty0<Int>>(C::mutable)

    accept<() -> Unit>(::<!UNRESOLVED_REFERENCE!>foo<!>)
    accept<(String) -> String>(::<!UNRESOLVED_REFERENCE!>bar<!>)
    accept<(String) -> Unit>(::<!UNRESOLVED_REFERENCE!>baz<!>)

    accept<() -> Int>(::<!UNRESOLVED_REFERENCE!>readonly<!>)
    accept<() -> Int>(::<!UNRESOLVED_REFERENCE!>mutable<!>)
    accept<KProperty0<Int>>(::<!UNRESOLVED_REFERENCE!>readonly<!>)
    accept<KProperty0<Int>>(::<!UNRESOLVED_REFERENCE!>mutable<!>)
    accept<KMutableProperty0<Int>>(::<!UNRESOLVED_REFERENCE!>mutable<!>)
}

fun <T> accept(t: T) {}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, functionDeclaration, functionalType, nullableType,
typeParameter */
