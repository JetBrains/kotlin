// RUN_PIPELINE_TILL: FRONTEND
// ALLOW_DANGEROUS_LANGUAGE_VERSION_TESTING
// LANGUAGE_VERSION: 2.0
// API_VERSION: 2.0

@RequiresOptIn
annotation class ApiMarker

@<!DEBUG_INFO_UNRESOLVED_WITH_TARGET, OPT_IN_USAGE_ERROR, UNRESOLVED_REFERENCE!>SubclassOptInRequired<!>(ApiMarker::class)
open class OpenKlassA

@OptIn(ExperimentalSubclassOptIn::class)
@SubclassOptInRequired(ApiMarker::class)
open class OpenKlassB
