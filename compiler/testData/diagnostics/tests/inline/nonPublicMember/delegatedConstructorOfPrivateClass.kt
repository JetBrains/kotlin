// RUN_PIPELINE_TILL: FRONTEND
// LATEST_LV_DIFFERENCE
// ISSUE: KT-76316
private abstract class C
<!NOTHING_TO_INLINE!>inline<!> fun f(): Any = object : <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>C<!>() {}
