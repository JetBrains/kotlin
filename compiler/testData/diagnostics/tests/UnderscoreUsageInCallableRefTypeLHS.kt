class `___` {
    class `____`
}

val testCallableRefLHSType = <!UNDERSCORE_USAGE_WITHOUT_BACKTICKS!>___<!>::toString
val testCallableRefLHSType2 = `___`::toString

val testClassLiteralLHSType = <!UNDERSCORE_USAGE_WITHOUT_BACKTICKS!>___<!>::class
val testClassLiteralLHSType2 = `___`::class

val tesLHSTypeFQN = `___`.<!UNDERSCORE_USAGE_WITHOUT_BACKTICKS!>____<!>::class