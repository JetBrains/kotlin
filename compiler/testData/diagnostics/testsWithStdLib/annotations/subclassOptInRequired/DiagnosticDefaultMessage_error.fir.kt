// RUN_PIPELINE_TILL: FRONTEND
@file:OptIn(ExperimentalSubclassOptIn::class)
@RequiresOptIn
annotation class ApiMarker

@SubclassOptInRequired(ApiMarker::class)
open class OpenKlass

@ApiMarker
open class OpenApiKlass

open class OpenKlassInheritor: <!OPT_IN_TO_INHERITANCE_ERROR("ApiMarker; This class or interface requires opt-in to be implemented. The implementation must be annotated with '@ApiMarker', '@OptIn(ApiMarker::class)' or '@SubclassOptInRequired(ApiMarker::class)'")!>OpenKlass<!>()
open class OpenApiKlassInheritor: <!OPT_IN_USAGE_ERROR("ApiMarker; This declaration requires opt-in to be used. The usage must be annotated with '@ApiMarker' or '@OptIn(ApiMarker::class)'")!>OpenApiKlass<!>()

fun check(klass: <!OPT_IN_USAGE_ERROR("ApiMarker; This declaration requires opt-in to be used. The usage must be annotated with '@ApiMarker' or '@OptIn(ApiMarker::class)'")!>OpenApiKlass<!>){}

class FinalImplA: <!OPT_IN_TO_INHERITANCE_ERROR("ApiMarker; This class or interface requires opt-in to be implemented. The implementation must be annotated with '@ApiMarker' or '@OptIn(ApiMarker::class)'")!>OpenKlass<!>()
