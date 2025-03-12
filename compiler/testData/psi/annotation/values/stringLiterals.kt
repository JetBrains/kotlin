// FILE: StringLiteral.kt
annotation class StringLiteral(
    val s1: String,
    val s2: String,
    val s3: String
)

const val CONSTANT = 0

// FILE: WithStringLiteral.kt
@StringLiteral("some", "", "H$CONSTANT")
class WithStringLiteral

// FILE: WithStringLiteralConcat.kt
@StringLiteral("some" + "1", "" + CONSTANT + "2", "$CONSTANT" + "3")
class WithStringLiteralConcat

// FILE: WithStringInterpolationPrefix.kt
@StringLiteral($"$CONSTANT", $$"$$CONSTANT", $$$"$$$CONSTANT")
class WithStringInterpolationPrefix
