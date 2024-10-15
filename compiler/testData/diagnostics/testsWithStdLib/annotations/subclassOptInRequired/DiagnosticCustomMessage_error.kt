// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
@file:OptIn(ExperimentalSubclassOptIn::class)

@RequiresOptIn(message = "API Unstable!")
annotation class ApiMarker

@SubclassOptInRequired(ApiMarker::class)
open class OpenKlass

@ApiMarker
open class OpenApiKlass

open class OpenKlassInheritor :
    <!OPT_IN_TO_INHERITANCE_ERROR("ApiMarker; This class or interface requires opt-in to be implemented: API Unstable!")!>OpenKlass<!>()
open class OpenApiKlassInheritor : <!OPT_IN_USAGE_ERROR("ApiMarker; API Unstable!")!>OpenApiKlass<!>()

fun check(klass: <!OPT_IN_USAGE_ERROR("ApiMarker; API Unstable!")!>OpenApiKlass<!>){}
