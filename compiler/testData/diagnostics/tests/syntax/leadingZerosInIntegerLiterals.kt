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
val singleLeadingZeros = <!INT_LITERAL_WITH_LEADING_ZEROS!>00<!>
val multipleLeadingZeros = <!INT_LITERAL_WITH_LEADING_ZEROS!>000000000000000000000001<!>
val longWithLeadingZeros = <!INT_LITERAL_WITH_LEADING_ZEROS!>002L<!>
val unsignedWithLeadingZeros = <!INT_LITERAL_WITH_LEADING_ZEROS!>0003U<!>
val unsignedLongWithLeadingZeros = <!INT_LITERAL_WITH_LEADING_ZEROS!>0004UL<!>
val underscoreAfterLeadingZero = <!INT_LITERAL_WITH_LEADING_ZEROS!>0_6<!>

/* GENERATED_FIR_TAGS: integerLiteral, propertyDeclaration */
