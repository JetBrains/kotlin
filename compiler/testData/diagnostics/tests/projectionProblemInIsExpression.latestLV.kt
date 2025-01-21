// RUN_PIPELINE_TILL: FRONTEND
// LATEST_LV_DIFFERENCE
// ISSUE: KT-67764

class CustomList<in T>{}

fun m(l: Any) {
    val asList = l is <!CANNOT_CHECK_FOR_ERASED!>CustomList<<!CONFLICTING_PROJECTION!>out<!> String><!>
}
