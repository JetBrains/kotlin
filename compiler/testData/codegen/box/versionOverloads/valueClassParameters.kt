// WORKS_WHEN_VALUE_CLASS
// WITH_STDLIB
@file:OptIn(ExperimentalVersionOverloading::class)
OPTIONAL_JVM_INLINE_ANNOTATION

value class VersionedToken(val value: String)

fun renderVersionedTokens(
    @IntroducedAt("1") token: VersionedToken = VersionedToken("K"),
    @IntroducedAt("2") nullableToken: VersionedToken? = null,
): String = token.value + (nullableToken?.value ?: "N")

fun box(): String {
    if (renderVersionedTokens() != "KN") return "FAIL default"
    if (renderVersionedTokens(VersionedToken("A")) != "AN") return "FAIL first explicit"
    if (renderVersionedTokens(VersionedToken("A"), VersionedToken("B")) != "AB") return "FAIL full explicit"
    return "OK"
}
