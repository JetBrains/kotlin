// FIR_IDENTICAL
@file:OptIn(ExperimentalSubclassOptIn::class)

@RequiresOptIn
annotation class ApiMarker

@SubclassOptInRequired(ApiMarker::class)
abstract class AbstractKlassOptInApi

// no opt-in: diagnostic reported
abstract class AbstractKlassOptInApiInheritorA: <!OPT_IN_USAGE_ERROR!>AbstractKlassOptInApi<!>()

// opt-in present: no diagnostic, opt-in isn't propagated
@OptIn(ApiMarker::class)
abstract class AbstractKlassOptInApiInheritorB: AbstractKlassOptInApi()

// inheritance opt-in required: no diagnostic, opt-in is propagated
@SubclassOptInRequired(ApiMarker::class)
abstract class AbstractKlassOptInApiInheritorC: AbstractKlassOptInApi()

// full opt-in required: no diagnostic, stricter opt-in is propagated
@ApiMarker
abstract class AbstractKlassOptInApiInheritorD: AbstractKlassOptInApi()

abstract class AbstractKlassOptInApiInheritorE: AbstractKlassOptInApiInheritorB() // inheritance opt-in isn't propagated
abstract class AbstractKlassOptInApiInheritorF: <!OPT_IN_USAGE_ERROR!>AbstractKlassOptInApiInheritorC<!>() // inheritance opt-in is propagated
abstract class AbstractKlassOptInApiInheritorG: <!OPT_IN_USAGE_ERROR!>AbstractKlassOptInApiInheritorD<!>() // inheritance opt-in is propagated

fun useSiteTestAbstractClasses(
    o: AbstractKlassOptInApi,           // usage opt-in isn't required
    b: AbstractKlassOptInApiInheritorB, // usage opt-in isn't required
    c: AbstractKlassOptInApiInheritorC, // usage opt-in isn't required
    d: <!OPT_IN_USAGE_ERROR!>AbstractKlassOptInApiInheritorD<!>, // usage opt-in is required
    e: AbstractKlassOptInApiInheritorE  // usage opt-in isn't required
) {}
