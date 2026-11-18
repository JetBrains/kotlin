// LIBRARY_PLATFORMS: JVM
// WITH_STDLIB

@JvmInline
value class NonNegative(val v: Int)

class NoDefault(val x: Int, @IntroducedAt("2") val n: NonNegative = NonNegative(0))

class Clash(val s: String = "s", @IntroducedAt("2") val n: NonNegative = NonNegative(1))

// LIGHT_ELEMENTS_NO_DECLARATION: Clash.class[getN-X68VEag], NoDefault.class[getN-X68VEag], NonNegative.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl]
