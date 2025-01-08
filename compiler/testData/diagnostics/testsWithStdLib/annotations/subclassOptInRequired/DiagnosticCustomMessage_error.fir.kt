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
    <!OPT_IN_TO_INHERITANCE_ERROR("ApiMarkerA; This class or interface requires opt-in to be implemented: API Unstable! The implementation must be annotated with '@ApiMarkerA', '@OptIn(ApiMarkerA::class)' or '@SubclassOptInRequired(ApiMarkerA::class)'")!>OpenKlassA<!>()
open class OpenApiKlassInheritorA : <!OPT_IN_USAGE_ERROR("ApiMarkerA; This declaration requires opt-in to be used: API Unstable! The usage must be annotated with '@ApiMarkerA' or '@OptIn(ApiMarkerA::class)'")!>OpenApiKlassA<!>()

fun check(klass: <!OPT_IN_USAGE_ERROR("ApiMarkerA; This declaration requires opt-in to be used: API Unstable! The usage must be annotated with '@ApiMarkerA' or '@OptIn(ApiMarkerA::class)'")!>OpenApiKlassA<!>){}

open class OpenKlassInheritorB :
    <!OPT_IN_TO_INHERITANCE_ERROR("ApiMarkerB; This class or interface requires opt-in to be implemented: API Unstable. The implementation must be annotated with '@ApiMarkerB', '@OptIn(ApiMarkerB::class)' or '@SubclassOptInRequired(ApiMarkerB::class)'")!>OpenKlassB<!>()
open class OpenApiKlassInheritorB : <!OPT_IN_USAGE_ERROR("ApiMarkerB; This declaration requires opt-in to be used: API Unstable. The usage must be annotated with '@ApiMarkerB' or '@OptIn(ApiMarkerB::class)'")!>OpenApiKlassB<!>()


open class OpenKlassInheritorС :
    <!OPT_IN_TO_INHERITANCE_ERROR("ApiMarkerC; This class or interface requires opt-in to be implemented: API Unstable? The implementation must be annotated with '@ApiMarkerC', '@OptIn(ApiMarkerC::class)' or '@SubclassOptInRequired(ApiMarkerC::class)'")!>OpenKlassC<!>()
open class OpenApiKlassInheritorС : <!OPT_IN_USAGE_ERROR("ApiMarkerC; This declaration requires opt-in to be used: API Unstable? The usage must be annotated with '@ApiMarkerC' or '@OptIn(ApiMarkerC::class)'")!>OpenApiKlassC<!>()

open class OpenKlassInheritorD:
    <!OPT_IN_TO_INHERITANCE_ERROR("ApiMarkerD; This class or interface requires opt-in to be implemented: API Unstable. The implementation must be annotated with '@ApiMarkerD', '@OptIn(ApiMarkerD::class)' or '@SubclassOptInRequired(ApiMarkerD::class)'")!>OpenKlassD<!>()
open class OpenApiKlassInheritorD : <!OPT_IN_USAGE_ERROR("ApiMarkerD; This declaration requires opt-in to be used: API Unstable. The usage must be annotated with '@ApiMarkerD' or '@OptIn(ApiMarkerD::class)'")!>OpenApiKlassD<!>()

open class OpenKlassInheritorE:
    <!OPT_IN_TO_INHERITANCE_ERROR("ApiMarkerE; This class or interface requires opt-in to be implemented. The implementation must be annotated with '@ApiMarkerE', '@OptIn(ApiMarkerE::class)' or '@SubclassOptInRequired(ApiMarkerE::class)'")!>OpenKlassE<!>()
open class OpenApiKlassInheritorE : <!OPT_IN_USAGE_ERROR("ApiMarkerE; This declaration requires opt-in to be used. The usage must be annotated with '@ApiMarkerE' or '@OptIn(ApiMarkerE::class)'")!>OpenApiKlassE<!>()
