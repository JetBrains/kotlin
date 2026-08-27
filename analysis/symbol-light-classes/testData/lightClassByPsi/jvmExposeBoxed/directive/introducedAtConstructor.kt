// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// JVM_EXPOSE_BOXED

@JvmInline
value class JvmMarker(val value: Int)

class IntroducedOnly(
    @IntroducedAt("3") val value: UInt = 1u,
)

class IntroducedAfterBase(
    val text: String = "text",
    @IntroducedAt("3") val value: UInt = 2u,
)

// LIGHT_ELEMENTS_NO_DECLARATION: IntroducedAfterBase.class[getValue-pVg5ArA], IntroducedOnly.class[getValue-pVg5ArA], JvmMarker.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl]
