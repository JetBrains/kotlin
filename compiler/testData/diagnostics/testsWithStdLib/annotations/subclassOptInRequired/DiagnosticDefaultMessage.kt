@file:OptIn(ExperimentalSubclassOptIn::class)
@RequiresOptIn
annotation class ApiMarker

@SubclassOptInRequired(ApiMarker::class)
open class OpenKlass

@ApiMarker
open class OpenApiKlass

open class OpenKlassInheritor: <!OPT_IN_USAGE_ERROR("ApiMarker; This declaration needs opt-in. Its usage must be marked with '@ApiMarker' or '@OptIn(ApiMarker::class)'")!>OpenKlass<!>()
open class OpenApiKlassInheritor: <!OPT_IN_USAGE_ERROR("ApiMarker; This declaration needs opt-in. Its usage must be marked with '@ApiMarker' or '@OptIn(ApiMarker::class)'")!>OpenApiKlass<!>()
