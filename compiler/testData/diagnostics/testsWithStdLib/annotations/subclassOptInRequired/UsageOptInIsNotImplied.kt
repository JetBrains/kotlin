// FIR_IDENTICAL
@file:OptIn(ExperimentalSubclassOptIn::class)

@RequiresOptIn
annotation class ApiMarker

@ApiMarker
class UnstableKlassApi

open class UnstableFunctionApi {
    @ApiMarker
    open fun overridableFunction() {}
}

@SubclassOptInRequired(ApiMarker::class)
open class NotFullyOptedIntoApiMarker: UnstableFunctionApi() {
    init {
        // usage is unstable, error is reported even despite SubclassOptInRequired
        <!OPT_IN_USAGE_ERROR!>UnstableKlassApi<!>()
    }
    // usage is unstable, error is reported even despite SubclassOptInRequired
    override fun <!OPT_IN_OVERRIDE_ERROR!>overridableFunction<!>() {}
}
