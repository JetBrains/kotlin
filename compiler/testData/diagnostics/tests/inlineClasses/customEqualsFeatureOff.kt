// WITH_STDLIB
// LANGUAGE: +ValueClasses
// TARGET_BACKEND: JVM_IR
// SKIP_TXT
// FIR_IDENTICAL

@JvmInline
value class IC(val x : Int) {
    <!UNSUPPORTED_FEATURE!>@TypedEquals<!>
    fun <!RESERVED_MEMBER_INSIDE_VALUE_CLASS!>equals<!>(other : IC) = true

    override fun <!RESERVED_MEMBER_INSIDE_VALUE_CLASS!>equals<!>(other: Any?) = true
}