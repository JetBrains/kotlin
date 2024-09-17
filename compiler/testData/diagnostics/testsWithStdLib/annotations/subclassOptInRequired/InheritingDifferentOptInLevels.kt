// FIR_IDENTICAL

@RequiresOptIn
annotation class ApiMarker

@ApiMarker
interface I1

@SubclassOptInRequired(ApiMarker::class)
interface I2

// SubclassOptInRequired can only fix the error from I2
@SubclassOptInRequired(ApiMarker::class)
interface Impl1: <!OPT_IN_USAGE_ERROR!>I1<!>, I2

// the order of I1 and I2 shouldn't matter
@SubclassOptInRequired(ApiMarker::class)
interface Impl2: I2, <!OPT_IN_USAGE_ERROR!>I1<!>
