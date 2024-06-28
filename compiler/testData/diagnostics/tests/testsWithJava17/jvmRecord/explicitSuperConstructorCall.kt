// FIR_IDENTICAL
// SKIP_TXT
// ISSUE: KT-54573
// JVM_TARGET: 17

@JvmRecord
data class A constructor(val x: Int, val s: String) {
    constructor(x: Long, s: String) : <!PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED!>super<!>(<!TOO_MANY_ARGUMENTS!>x.toInt()<!>, <!TOO_MANY_ARGUMENTS!>s<!>)
    constructor(x: Byte, s: String) : <!PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED!>super<!>(<!TOO_MANY_ARGUMENTS!>x.toInt()<!>, <!TOO_MANY_ARGUMENTS!>s.<!UNRESOLVED_REFERENCE!>unresolved<!>()<!>)

    constructor(s: String) : this(s.length, s)
    constructor(s: CharSequence) : this(s.length, s.<!UNRESOLVED_REFERENCE!>unresolved<!>())
}
