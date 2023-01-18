package kotlin.io

import kotlin.internal.*

@<!INVISIBLE_REFERENCE, INVISIBLE_REFERENCE!>RequireKotlin<!>(<!ILLEGAL_KOTLIN_VERSION_STRING_VALUE!>"1.x"<!>)
class IllegalVersion()

@<!INVISIBLE_REFERENCE, INVISIBLE_REFERENCE!>RequireKotlin<!>("1.2")
class LegalMinimum()

@<!INVISIBLE_REFERENCE, INVISIBLE_REFERENCE!>RequireKotlin<!>("1.2", versionKind = <!INVISIBLE_REFERENCE, NO_COMPANION_OBJECT!>RequireKotlinVersionKind<!>.<!INVISIBLE_REFERENCE!>COMPILER_VERSION<!>, message = "Requires newer compiler version to be inlined correctly.")
class LegalStdLib()
