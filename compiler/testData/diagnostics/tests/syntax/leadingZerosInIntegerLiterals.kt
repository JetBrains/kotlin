// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// ISSUE: KT-5486

// Allowed
val singleZero = 0
val hexLiteral = 0xF
val doubleLiteral = 0.1
val doubleLiteralWithLeadingZeroes = 000.555 // It's allowed as in Java

// Disallowed
val singleLeadingZeros = 0<!SYNTAX, UNSUPPORTED!>0<!>
val multipleLeadingZeros = 0<!SYNTAX!><!UNSUPPORTED!>0<!>0000000000000000000001<!>
val longWithLeadingZeros = 0<!SYNTAX!><!UNSUPPORTED!>0<!>2L<!>
val unsignedWithLeadingZeros = 0<!SYNTAX!><!UNSUPPORTED!>0<!>03U<!>
val unsignedLongWithLeadingZeros = 0<!SYNTAX!><!UNSUPPORTED!>0<!>04UL<!>
val underscoreAfterLeadingZero = 0<!UNRESOLVED_REFERENCE, UNSUPPORTED!>_6<!><!SYNTAX!><!>

/* GENERATED_FIR_TAGS: integerLiteral, propertyDeclaration */
