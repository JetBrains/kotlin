// FIR_IDENTICAL

@RequiresOptIn
annotation class ApiMarker

@<!OPT_IN_USAGE_ERROR!>SubclassOptInRequired<!>(ApiMarker::class)
open class OpenKlass
