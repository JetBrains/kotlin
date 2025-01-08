// RUN_PIPELINE_TILL: FRONTEND
@file:OptIn(ExperimentalSubclassOptIn::class)

@RequiresOptIn(message = "API Unstable!")
annotation class ApiMarkerA

@RequiresOptIn(message = "API Unstable.")
annotation class ApiMarkerB

@RequiresOptIn(message = "API Unstable?")
annotation class ApiMarkerC

@RequiresOptIn(message = "API Unstable")
annotation class ApiMarkerD

@RequiresOptIn(message = "")
annotation class ApiMarkerE

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

@SubclassOptInRequired(ApiMarkerE::class)
open class OpenKlassE

@ApiMarkerE
open class OpenApiKlassE

open class OpenKlassInheritorA :
    <!OPT_IN_TO_INHERITANCE_ERROR("ApiMarkerA; This class or interface requires opt-in to be implemented: API Unstable!")!>OpenKlassA<!>()
open class OpenApiKlassInheritorA : <!OPT_IN_USAGE_ERROR("ApiMarkerA; API Unstable!")!>OpenApiKlassA<!>()

fun check(klass: <!OPT_IN_USAGE_ERROR("ApiMarkerA; API Unstable!")!>OpenApiKlassA<!>){}

open class OpenKlassInheritorB :
    <!OPT_IN_TO_INHERITANCE_ERROR("ApiMarkerB; This class or interface requires opt-in to be implemented: API Unstable.")!>OpenKlassB<!>()
open class OpenApiKlassInheritorB : <!OPT_IN_USAGE_ERROR("ApiMarkerB; API Unstable.")!>OpenApiKlassB<!>()


open class OpenKlassInheritorС :
    <!OPT_IN_TO_INHERITANCE_ERROR("ApiMarkerC; This class or interface requires opt-in to be implemented: API Unstable?")!>OpenKlassC<!>()
open class OpenApiKlassInheritorС : <!OPT_IN_USAGE_ERROR("ApiMarkerC; API Unstable?")!>OpenApiKlassC<!>()

open class OpenKlassInheritorD:
    <!OPT_IN_TO_INHERITANCE_ERROR("ApiMarkerD; This class or interface requires opt-in to be implemented: API Unstable")!>OpenKlassD<!>()
open class OpenApiKlassInheritorD : <!OPT_IN_USAGE_ERROR("ApiMarkerD; API Unstable")!>OpenApiKlassD<!>()

open class OpenKlassInheritorE:
    <!OPT_IN_TO_INHERITANCE_ERROR("ApiMarkerE; This class or interface requires opt-in to be implemented. Its usage must be marked with '@ApiMarkerE', '@OptIn(ApiMarkerE::class)' or '@SubclassOptInRequired(ApiMarkerE::class)'")!>OpenKlassE<!>()
open class OpenApiKlassInheritorE : <!OPT_IN_USAGE_ERROR("ApiMarkerE; This declaration needs opt-in. Its usage must be marked with '@ApiMarkerE' or '@OptIn(ApiMarkerE::class)'")!>OpenApiKlassE<!>()
