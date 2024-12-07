// RUN_PIPELINE_TILL: BACKEND
@file:OptIn(ExperimentalSubclassOptIn::class)
@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
annotation class ApiMarker

@SubclassOptInRequired(ApiMarker::class)
open class OpenKlass

@ApiMarker
open class OpenApiKlass

open class OpenKlassInheritor: <!OPT_IN_TO_INHERITANCE("ApiMarker; This class or interface requires opt-in to be implemented. Its usage should be marked with '@ApiMarker', '@OptIn(ApiMarker::class)' or '@SubclassOptInRequired(ApiMarker::class)'")!>OpenKlass<!>()
open class OpenApiKlassInheritor: <!OPT_IN_USAGE("ApiMarker; This declaration needs opt-in. Its usage should be marked with '@ApiMarker' or '@OptIn(ApiMarker::class)'")!>OpenApiKlass<!>()

fun check(klass: <!OPT_IN_USAGE("ApiMarker; This declaration needs opt-in. Its usage should be marked with '@ApiMarker' or '@OptIn(ApiMarker::class)'")!>OpenApiKlass<!>){}

class FinalImplA: <!OPT_IN_TO_INHERITANCE("ApiMarker; This class or interface requires opt-in to be implemented. Its usage should be marked with '@ApiMarker' or '@OptIn(ApiMarker::class)'")!>OpenKlass<!>()
