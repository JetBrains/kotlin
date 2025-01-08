// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
@file:OptIn(ExperimentalSubclassOptIn::class)

@RequiresOptIn(message = "API Unstable!", level = RequiresOptIn.Level.WARNING)
annotation class ApiMarkerA

@RequiresOptIn(message = "API Unstable.", level = RequiresOptIn.Level.WARNING)
annotation class ApiMarkerB

@RequiresOptIn(message = "API Unstable?", level = RequiresOptIn.Level.WARNING)
annotation class ApiMarkerC

@RequiresOptIn(message = "API Unstable", level = RequiresOptIn.Level.WARNING)
annotation class ApiMarkerD

@SubclassOptInRequired(ApiMarkerA::class)
open class OpenKlassA

@ApiMarkerA
open class OpenApiKlassA

@SubclassOptInRequired(ApiMarkerB::class)
open class OpenKlassB

@ApiMarkerB
open class OpenApiKlassB

@SubclassOptInRequired(ApiMarkerC::class)
open class OpenKlassC

@ApiMarkerC
open class OpenApiKlassC

@SubclassOptInRequired(ApiMarkerD::class)
open class OpenKlassD

@ApiMarkerD
open class OpenApiKlassD

open class OpenKlassInheritorA :
    <!OPT_IN_TO_INHERITANCE("ApiMarkerA; This class or interface requires opt-in to be implemented: API Unstable!")!>OpenKlassA<!>()
open class OpenApiKlassInheritorA :
    <!OPT_IN_USAGE("ApiMarkerA; API Unstable!")!>OpenApiKlassA<!>()

fun check(klass: <!OPT_IN_USAGE("ApiMarkerA; API Unstable!")!>OpenApiKlassA<!>){}

open class OpenKlassInheritorB :
    <!OPT_IN_TO_INHERITANCE("ApiMarkerB; This class or interface requires opt-in to be implemented: API Unstable.")!>OpenKlassB<!>()
open class OpenApiKlassInheritorB :
    <!OPT_IN_USAGE("ApiMarkerB; API Unstable.")!>OpenApiKlassB<!>()


open class OpenKlassInheritorС :
    <!OPT_IN_TO_INHERITANCE("ApiMarkerC; This class or interface requires opt-in to be implemented: API Unstable?")!>OpenKlassC<!>()
open class OpenApiKlassInheritorС :
    <!OPT_IN_USAGE("ApiMarkerC; API Unstable?")!>OpenApiKlassC<!>()

open class OpenKlassInheritorD :
    <!OPT_IN_TO_INHERITANCE("ApiMarkerD; This class or interface requires opt-in to be implemented: API Unstable")!>OpenKlassD<!>()
open class OpenApiKlassInheritorD :
    <!OPT_IN_USAGE("ApiMarkerD; API Unstable")!>OpenApiKlassD<!>()
