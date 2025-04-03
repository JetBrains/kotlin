// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-57274, KT-70352, KT-76209

// CONFLICTING_UPPER_BOUND should not be reported as having
// Nothing bounds is useful for some scenarios.
fun <T: <!FINAL_UPPER_BOUND!>Nothing<!>> f() {}
