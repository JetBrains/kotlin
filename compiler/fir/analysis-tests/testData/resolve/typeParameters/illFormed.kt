// ISSUE: KT-65368

open class AtomicReferenceArray<<!SYNTAX!>'T1> {<!>
    <!SYNTAX!>open<!> fun compareAndExchange(p0: Int): <!UNRESOLVED_REFERENCE!>T1<!> =
        AtomicReferenceArray<<!UNRESOLVED_REFERENCE!>T1<!>>().<!UNRESOLVED_REFERENCE!>compareAndExchange<!>(1)
<!SYNTAX!>}<!>
