// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM

@OptIn(ExperimentalStdlibApi::class)
@JvmInline
@JvmExposeBoxed
value class Money(val cents: Int) {
    operator fun plus(other: Money): Money = Money(cents + other.cents)

    infix fun combineWith(other: Money): Money = Money(cents * other.cents)
}

// LIGHT_ELEMENTS_NO_DECLARATION: Money.class[combineWith-YPPxQr0;constructor-impl;equals-impl;equals-impl0;hashCode-impl;plus-YPPxQr0;toString-impl]
