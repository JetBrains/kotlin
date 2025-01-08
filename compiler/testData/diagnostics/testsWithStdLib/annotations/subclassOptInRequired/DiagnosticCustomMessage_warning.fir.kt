// RUN_PIPELINE_TILL: BACKEND
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
    <!OPT_IN_TO_INHERITANCE("ApiMarkerA; This class or interface requires opt-in to be implemented: API Unstable! The implementation should be annotated with '@ApiMarkerA', '@OptIn(ApiMarkerA::class)' or '@SubclassOptInRequired(ApiMarkerA::class)'")!>OpenKlassA<!>()
open class OpenApiKlassInheritorA :
    <!OPT_IN_USAGE("ApiMarkerA; This declaration requires opt-in to be used: API Unstable! The usage should be annotated with '@ApiMarkerA' or '@OptIn(ApiMarkerA::class)'")!>OpenApiKlassA<!>()

fun check(klass: <!OPT_IN_USAGE("ApiMarkerA; This declaration requires opt-in to be used: API Unstable! The usage should be annotated with '@ApiMarkerA' or '@OptIn(ApiMarkerA::class)'")!>OpenApiKlassA<!>){}

open class OpenKlassInheritorB :
    <!OPT_IN_TO_INHERITANCE("ApiMarkerB; This class or interface requires opt-in to be implemented: API Unstable. The implementation should be annotated with '@ApiMarkerB', '@OptIn(ApiMarkerB::class)' or '@SubclassOptInRequired(ApiMarkerB::class)'")!>OpenKlassB<!>()
open class OpenApiKlassInheritorB :
    <!OPT_IN_USAGE("ApiMarkerB; This declaration requires opt-in to be used: API Unstable. The usage should be annotated with '@ApiMarkerB' or '@OptIn(ApiMarkerB::class)'")!>OpenApiKlassB<!>()


open class OpenKlassInheritorС :
    <!OPT_IN_TO_INHERITANCE("ApiMarkerC; This class or interface requires opt-in to be implemented: API Unstable? The implementation should be annotated with '@ApiMarkerC', '@OptIn(ApiMarkerC::class)' or '@SubclassOptInRequired(ApiMarkerC::class)'")!>OpenKlassC<!>()
open class OpenApiKlassInheritorС :
    <!OPT_IN_USAGE("ApiMarkerC; This declaration requires opt-in to be used: API Unstable? The usage should be annotated with '@ApiMarkerC' or '@OptIn(ApiMarkerC::class)'")!>OpenApiKlassC<!>()

open class OpenKlassInheritorD :
    <!OPT_IN_TO_INHERITANCE("ApiMarkerD; This class or interface requires opt-in to be implemented: API Unstable. The implementation should be annotated with '@ApiMarkerD', '@OptIn(ApiMarkerD::class)' or '@SubclassOptInRequired(ApiMarkerD::class)'")!>OpenKlassD<!>()
open class OpenApiKlassInheritorD :
    <!OPT_IN_USAGE("ApiMarkerD; This declaration requires opt-in to be used: API Unstable. The usage should be annotated with '@ApiMarkerD' or '@OptIn(ApiMarkerD::class)'")!>OpenApiKlassD<!>()
