// !LANGUAGE: +EnumEntries
// FIR_DUMP

enum class <!REDECLARATION!>Ambiguous<!> {
    first, <!REDECLARATION!>entries<!>;
}

val e = Ambiguous.<!OVERLOAD_RESOLUTION_AMBIGUITY!>entries<!>
