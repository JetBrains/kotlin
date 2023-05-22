// FIR_IDENTICAL

@RequiresOptIn
annotation class ApiMarker

@SubclassOptInRequired(ApiMarker::class)
open class OpenKlassOptInApi

// no opt-in: diagnostic reported
open class OpenKlassOptInApiInheritorA: <!OPT_IN_USAGE_ERROR!>OpenKlassOptInApi<!>()

// opt-in present: no diagnostic, opt-in isn't propagated
@OptIn(ApiMarker::class)
open class OpenKlassOptInApiInheritorB: OpenKlassOptInApi()

// inheritance opt-in required: no diagnostic, opt-in is propagated
@SubclassOptInRequired(ApiMarker::class)
open class OpenKlassOptInApiInheritorC: OpenKlassOptInApi()

// full opt-in required: no diagnostic, stricter opt-in is propagated
@ApiMarker
open class OpenKlassOptInApiInheritorD: OpenKlassOptInApi()

open class OpenKlassOptInApiInheritorE: OpenKlassOptInApiInheritorB() // inheritance opt-in isn't propagated
open class OpenKlassOptInApiInheritorF: <!OPT_IN_USAGE_ERROR!>OpenKlassOptInApiInheritorC<!>() // inheritance opt-in is propagated
open class OpenKlassOptInApiInheritorG: <!OPT_IN_USAGE_ERROR!>OpenKlassOptInApiInheritorD<!>() // inheritance opt-in is propagated

fun useSiteTestOpenClasses() {
    OpenKlassOptInApi()           // usage opt-in isn't required
    OpenKlassOptInApiInheritorB() // usage opt-in isn't required
    OpenKlassOptInApiInheritorC() // usage opt-in isn't required
    <!OPT_IN_USAGE_ERROR!>OpenKlassOptInApiInheritorD<!>() // usage opt-in is required
    OpenKlassOptInApiInheritorE() // usage opt-in isn't required
}
