// FIR_IDENTICAL
@file:OptIn(ExperimentalSubclassOptIn::class)

@RequiresOptIn
annotation class ApiMarker

@SubclassOptInRequired(ApiMarker::class)
interface InterfaceOptInApi

// no opt-in: diagnostic reported
interface InterfaceOptInApiInheritorA: <!OPT_IN_USAGE_ERROR!>InterfaceOptInApi<!>

// opt-in present: no diagnostic, opt-in isn't propagated
@OptIn(ApiMarker::class)
interface InterfaceOptInApiInheritorB: InterfaceOptInApi

// inheritance opt-in required: no diagnostic, opt-in is propagated
@SubclassOptInRequired(ApiMarker::class)
interface InterfaceOptInApiInheritorC: InterfaceOptInApi

// full opt-in required: no diagnostic, stricter opt-in is propagated
@ApiMarker
interface InterfaceOptInApiInheritorD: InterfaceOptInApi

interface InterfaceOptInApiInheritorE: InterfaceOptInApiInheritorB // inheritance opt-in isn't propagated
interface InterfaceOptInApiInheritorF: <!OPT_IN_USAGE_ERROR!>InterfaceOptInApiInheritorC<!> // inheritance opt-in is propagated
interface InterfaceOptInApiInheritorG: <!OPT_IN_USAGE_ERROR!>InterfaceOptInApiInheritorD<!> // inheritance opt-in is propagated

fun useSiteTestInterfaces(
    o: InterfaceOptInApi,           // usage opt-in isn't required
    b: InterfaceOptInApiInheritorB, // usage opt-in isn't required
    c: InterfaceOptInApiInheritorC, // usage opt-in isn't required
    d: <!OPT_IN_USAGE_ERROR!>InterfaceOptInApiInheritorD<!>, // usage opt-in is required
    e: InterfaceOptInApiInheritorE  // usage opt-in isn't required
) {}
