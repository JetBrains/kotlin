package kotlin.io

import kotlin.internal.*

<!ILLEGAL_KOTLIN_VERSION_STRING_VALUE!>@<!INVISIBLE_MEMBER, INVISIBLE_REFERENCE!>RequireKotlin<!>("1.x")<!>
class IllegalVersion()

@<!INVISIBLE_MEMBER, INVISIBLE_REFERENCE!>RequireKotlin<!>("1.2")
class LegalMinimum()

@<!INVISIBLE_MEMBER, INVISIBLE_REFERENCE!>RequireKotlin<!>("1.2", versionKind = <!INVISIBLE_REFERENCE!>RequireKotlinVersionKind<!>.<!INVISIBLE_MEMBER!>COMPILER_VERSION<!>, message = "Requires newer compiler version to be inlined correctly.")
class LegalStdLib()
